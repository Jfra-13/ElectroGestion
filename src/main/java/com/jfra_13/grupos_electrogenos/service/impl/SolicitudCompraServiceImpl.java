package com.jfra_13.grupos_electrogenos.service.impl;

import com.jfra_13.grupos_electrogenos.exception.ResourceNotFoundException;
import com.jfra_13.grupos_electrogenos.exception.StockInsuficienteException;
import com.jfra_13.grupos_electrogenos.model.dto.RankingEntidadDTO;
import com.jfra_13.grupos_electrogenos.model.dto.RankingVendedorDTO;
import com.jfra_13.grupos_electrogenos.model.dto.ReportePagoDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraResponseDTO;
import com.jfra_13.grupos_electrogenos.model.dto.PaginatedResponseDTO;
import com.jfra_13.grupos_electrogenos.model.entity.Entidad;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.SolicitudCompra;
import com.jfra_13.grupos_electrogenos.model.entity.Usuario;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import com.jfra_13.grupos_electrogenos.mapper.SolicitudCompraMapper;
import com.jfra_13.grupos_electrogenos.repository.EntidadRepository;
import com.jfra_13.grupos_electrogenos.repository.GrupoElectrogenoRepository;
import com.jfra_13.grupos_electrogenos.repository.SolicitudCompraRepository;
import com.jfra_13.grupos_electrogenos.repository.UsuarioRepository;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
import com.jfra_13.grupos_electrogenos.service.SolicitudCompraService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SolicitudCompraServiceImpl implements SolicitudCompraService {

    private final SolicitudCompraRepository repository;
    private final GrupoElectrogenoService grupoService;
    private final EntidadRepository entidadRepository;
    private final GrupoElectrogenoRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudCompraMapper mapper;

    public SolicitudCompraServiceImpl(SolicitudCompraRepository repository,
                                      GrupoElectrogenoService grupoService,
                                      EntidadRepository entidadRepository,
                                      GrupoElectrogenoRepository grupoRepository,
                                      UsuarioRepository usuarioRepository,
                                      SolicitudCompraMapper mapper) {
        this.repository = repository;
        this.grupoService = grupoService;
        this.entidadRepository = entidadRepository;
        this.grupoRepository = grupoRepository;
        this.usuarioRepository = usuarioRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public SolicitudCompraResponseDTO crearSolicitud(SolicitudCompraRequestDTO dto) {
        Entidad entidad = entidadRepository.findById(dto.getEntidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Entidad no encontrada"));

        List<GrupoElectrogeno> candidatos = grupoRepository.findByTipoCombustibleOrderByPMaxDesc(dto.getTipoCombustible(), Pageable.unpaged()).getContent();

        GrupoElectrogeno grupoSeleccionado = candidatos.stream()
                .filter(g -> g.getPMax() >= dto.getPotenciaRequerida())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un Grupo Electrógeno que cumpla con la potencia requerida para este combustible"));

        // I5: validar disponibilidad y descontar stock (rechaza si no alcanza).
        // El grupo está gestionado en esta transacción; @Version evita lost updates concurrentes.
        int disponible = grupoSeleccionado.getStock() != null ? grupoSeleccionado.getStock() : 0;
        if (disponible < dto.getCantidad()) {
            throw new StockInsuficienteException(
                    "Stock insuficiente para el grupo " + grupoSeleccionado.getCodigo()
                            + ": disponible " + disponible + ", solicitado " + dto.getCantidad());
        }
        grupoSeleccionado.setStock(disponible - dto.getCantidad());

        SolicitudCompra solicitud = mapper.toEntity(dto, entidad, grupoSeleccionado, UUID.randomUUID().toString().substring(0, 8));

        // El vendedor sale SIEMPRE del usuario autenticado, nunca del request:
        // así un empleado no puede atribuir la venta a otro.
        solicitud.setVendedor(usuarioAutenticado());

        // I4: congelar precio unitario y total al momento de la venta.
        double unitario = grupoService.calcularPrecioVenta(grupoSeleccionado);
        solicitud.setPrecioUnitario(unitario);
        solicitud.setTotal(unitario * dto.getCantidad());

        SolicitudCompra guardada = repository.save(solicitud);
        return mapper.toResponse(guardada);
    }

    /** Usuario autenticado actual. En producción siempre existe (lo carga el filtro JWT). */
    private Usuario usuarioAutenticado() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "El usuario autenticado '" + username + "' no existe en la base de datos."));
    }

    /** True si el usuario autenticado tiene ROLE_ADMIN. */
    private boolean esAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Override
    public SolicitudCompraResponseDTO obtenerPorId(Long id) {
        SolicitudCompra entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de compra no encontrada"));

        // Un empleado solo puede ver sus propias ventas. Devolvemos 404 (no 403)
        // para no revelar que la venta existe.
        if (!esAdmin()) {
            Usuario actual = usuarioAutenticado();
            boolean esDueño = entidad.getVendedor() != null
                    && entidad.getVendedor().getId().equals(actual.getId());
            if (!esDueño) {
                throw new ResourceNotFoundException("Solicitud de compra no encontrada");
            }
        }
        return mapper.toResponse(entidad);
    }

    @Override
    @Transactional
    public SolicitudCompraResponseDTO actualizarSolicitud(Long id, SolicitudCompraRequestDTO dto) {
        SolicitudCompra existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de compra no encontrada"));
        
        Entidad entidad = entidadRepository.findById(dto.getEntidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Entidad no encontrada"));

        // Re-evaluar grupo si cambian potencia requerida o tipo de combustible
        if (!existing.getTipoCombustible().equals(dto.getTipoCombustible()) || 
            !existing.getPotenciaRequerida().equals(dto.getPotenciaRequerida())) {
            
            List<GrupoElectrogeno> candidatos = grupoRepository.findByTipoCombustibleOrderByPMaxDesc(dto.getTipoCombustible(), Pageable.unpaged()).getContent();
            GrupoElectrogeno nuevoGrupo = candidatos.stream()
                .filter(g -> g.getPMax() >= dto.getPotenciaRequerida())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un Grupo Electrógeno que cumpla con la nueva potencia requerida para este combustible"));
            
            existing.setGrupoElectrogeno(nuevoGrupo);
        }

        mapper.updateEntity(dto, entidad, existing.getGrupoElectrogeno(), existing);

        // El precio unitario queda CONGELADO desde la creación; solo se recalcula el
        // total si cambió la cantidad. (Fallback para filas legacy sin precio congelado.)
        if (existing.getPrecioUnitario() == null) {
            existing.setPrecioUnitario(grupoService.calcularPrecioVenta(existing.getGrupoElectrogeno()));
        }
        existing.setTotal(existing.getPrecioUnitario() * existing.getCantidad());

        SolicitudCompra actualizada = repository.save(existing);
        return mapper.toResponse(actualizada);
    }

    @Override
    @Transactional
    public void eliminarSolicitud(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Solicitud de compra no encontrada");
        }
        repository.deleteById(id);
    }

    @Override
    public List<RankingEntidadDTO> obtenerRankingClientes() {
        return repository.obtenerRankingEntidades();
    }

    @Override
    public List<ReportePagoDTO> obtenerReportePorPago(TipoPago tipoPago) {
        return repository.obtenerReportePorTipoPago(tipoPago);
    }

    @Override
    public Double calcularIngresosTotales() {
        // I4: se suman los totales CONGELADOS de cada venta; no se recalcula desde el grupo.
        return repository.findAll().stream()
                .mapToDouble(venta -> venta.getTotal() != null ? venta.getTotal() : 0.0)
                .sum();
    }

    @Override
    public List<RankingVendedorDTO> obtenerRankingVendedores() {
        return repository.obtenerRankingVendedores();
    }

    @Override
    public PaginatedResponseDTO<SolicitudCompraResponseDTO> listarVentasPaginado(Pageable pageable, Long vendedorId) {
        Page<SolicitudCompra> page;
        if (esAdmin()) {
            // El jefe ve todas, o las de un empleado puntual si filtra por vendedorId.
            page = (vendedorId != null)
                    ? repository.findByVendedorId(vendedorId, pageable)
                    : repository.findAll(pageable);
        } else {
            // El empleado solo ve las suyas; se ignora cualquier vendedorId del request.
            page = repository.findByVendedorId(usuarioAutenticado().getId(), pageable);
        }

        List<SolicitudCompraResponseDTO> content = page.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        return new PaginatedResponseDTO<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
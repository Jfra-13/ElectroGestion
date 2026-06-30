package com.jfra_13.grupos_electrogenos.service.impl;

import com.jfra_13.grupos_electrogenos.exception.ResourceNotFoundException;
import com.jfra_13.grupos_electrogenos.exception.StockInsuficienteException;
import com.jfra_13.grupos_electrogenos.exception.VentaYaAnuladaException;
import com.jfra_13.grupos_electrogenos.model.dto.AnulacionRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.RankingEntidadDTO;
import com.jfra_13.grupos_electrogenos.model.dto.RankingVendedorDTO;
import com.jfra_13.grupos_electrogenos.model.dto.ReportePagoDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraUpdateDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraResponseDTO;
import com.jfra_13.grupos_electrogenos.model.dto.PaginatedResponseDTO;
import com.jfra_13.grupos_electrogenos.model.entity.Entidad;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.SolicitudCompra;
import com.jfra_13.grupos_electrogenos.model.entity.Usuario;
import com.jfra_13.grupos_electrogenos.model.enums.EstadoVenta;
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

import java.time.LocalDateTime;
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

        GrupoElectrogeno grupoSeleccionado;
        if (dto.getGrupoCodigo() != null && !dto.getGrupoCodigo().isBlank()) {
            // Venta por grupo elegido: el grupo ya está decidido, NO se corre la
            // tasación. Se vende ese código exacto, con su stock y su precio.
            grupoSeleccionado = grupoRepository.findByCodigo(dto.getGrupoCodigo())
                    .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado: " + dto.getGrupoCodigo()));
        } else {
            // Sin grupoCodigo: tasación actual (elige el grupo que cumple los criterios).
            List<GrupoElectrogeno> candidatos = grupoRepository.findByTipoCombustibleOrderByPMaxDesc(dto.getTipoCombustible(), Pageable.unpaged()).getContent();
            grupoSeleccionado = candidatos.stream()
                    .filter(g -> g.getPMax() >= dto.getPotenciaRequerida())
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró un Grupo Electrógeno que cumpla con la potencia requerida para este combustible"));
        }

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
    public SolicitudCompraResponseDTO actualizarSolicitud(Long id, SolicitudCompraUpdateDTO dto) {
        SolicitudCompra existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de compra no encontrada"));

        // No se edita una venta anulada: está cerrada, es solo rastro.
        if (existing.getEstado() == EstadoVenta.ANULADA) {
            throw new VentaYaAnuladaException("No se puede editar una venta anulada.");
        }

        // Edición ACOTADA: solo el nombre del solicitante. tipoPago, cantidad,
        // potencia, combustible, grupo, entidad, precio y total son inmutables;
        // tocarlos descuadraría stock y reportes ya emitidos. Para cambiarlos:
        // anular la venta y registrar una nueva.
        existing.setNombreSolicitante(dto.getNombreSolicitante());

        SolicitudCompra actualizada = repository.save(existing);
        return mapper.toResponse(actualizada);
    }

    @Override
    @Transactional
    public SolicitudCompraResponseDTO anularVenta(Long id, AnulacionRequestDTO dto) {
        SolicitudCompra venta = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de compra no encontrada"));

        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new VentaYaAnuladaException("La venta ya estaba anulada.");
        }

        // Reponer stock: el grupo está gestionado en esta transacción, @Version cubre
        // concurrencia. El dirty checking persiste el cambio al cerrar la transacción.
        GrupoElectrogeno grupo = venta.getGrupoElectrogeno();
        int stockActual = grupo.getStock() != null ? grupo.getStock() : 0;
        grupo.setStock(stockActual + venta.getCantidad());

        // Marcar anulada con rastro. El precio/total quedan congelados tal cual se
        // había cobrado: lo que la saca de la caja es el estado, no borrar el número.
        venta.setEstado(EstadoVenta.ANULADA);
        venta.setAnuladaAt(LocalDateTime.now());
        venta.setAnuladaPor(usuarioAutenticado());
        venta.setMotivoAnulacion(dto.getMotivo());

        SolicitudCompra anulada = repository.save(venta);
        return mapper.toResponse(anulada);
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
        // I4: se suman los totales CONGELADOS de las ventas ACTIVA; las anuladas no
        // cuentan. Agregado en la DB (SUM) en vez de traer todo a memoria.
        return repository.sumarIngresosActivas();
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
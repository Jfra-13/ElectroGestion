package com.jfra_13.grupos_electrogenos.service.impl;

import com.jfra_13.grupos_electrogenos.exception.ResourceNotFoundException;
import com.jfra_13.grupos_electrogenos.model.dto.RankingEntidadDTO;
import com.jfra_13.grupos_electrogenos.model.dto.ReportePagoDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraResponseDTO;
import com.jfra_13.grupos_electrogenos.model.dto.PaginatedResponseDTO;
import com.jfra_13.grupos_electrogenos.model.entity.Entidad;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.SolicitudCompra;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import com.jfra_13.grupos_electrogenos.mapper.SolicitudCompraMapper;
import com.jfra_13.grupos_electrogenos.repository.EntidadRepository;
import com.jfra_13.grupos_electrogenos.repository.GrupoElectrogenoRepository;
import com.jfra_13.grupos_electrogenos.repository.SolicitudCompraRepository;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
import com.jfra_13.grupos_electrogenos.service.SolicitudCompraService;
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
    private final SolicitudCompraMapper mapper;

    public SolicitudCompraServiceImpl(SolicitudCompraRepository repository, 
                                      GrupoElectrogenoService grupoService, 
                                      EntidadRepository entidadRepository,
                                      GrupoElectrogenoRepository grupoRepository,
                                      SolicitudCompraMapper mapper) {
        this.repository = repository;
        this.grupoService = grupoService;
        this.entidadRepository = entidadRepository;
        this.grupoRepository = grupoRepository;
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

        SolicitudCompra solicitud = mapper.toEntity(dto, entidad, grupoSeleccionado, UUID.randomUUID().toString().substring(0, 8));

        SolicitudCompra guardada = repository.save(solicitud);
        return mapper.toResponse(guardada, grupoService.calcularPrecioVenta(guardada.getGrupoElectrogeno()));
    }

    @Override
    public SolicitudCompraResponseDTO obtenerPorId(Long id) {
        SolicitudCompra entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de compra no encontrada"));
        return mapper.toResponse(entidad, grupoService.calcularPrecioVenta(entidad.getGrupoElectrogeno()));
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

        SolicitudCompra actualizada = repository.save(existing);
        return mapper.toResponse(actualizada, grupoService.calcularPrecioVenta(actualizada.getGrupoElectrogeno()));
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
        return repository.findAll().stream()
                .mapToDouble(venta -> grupoService.calcularPrecioVenta(venta.getGrupoElectrogeno()) * venta.getCantidad())
                .sum();
    }

    @Override
    public PaginatedResponseDTO<SolicitudCompraResponseDTO> listarVentasPaginado(Pageable pageable) {
        Page<SolicitudCompra> page = repository.findAll(pageable);
        List<SolicitudCompraResponseDTO> content = page.stream()
                .map(venta -> mapper.toResponse(venta, grupoService.calcularPrecioVenta(venta.getGrupoElectrogeno())))
                .collect(Collectors.toList());

        return new PaginatedResponseDTO<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
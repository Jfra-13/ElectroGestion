package com.jfra_13.grupos_electrogenos.service.impl;

import com.jfra_13.grupos_electrogenos.exception.ResourceNotFoundException;
import com.jfra_13.grupos_electrogenos.model.dto.RankingEntidadDTO;
import com.jfra_13.grupos_electrogenos.model.dto.ReportePagoDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraResponseDTO;
import com.jfra_13.grupos_electrogenos.model.entity.Entidad;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.SolicitudCompra;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import com.jfra_13.grupos_electrogenos.repository.EntidadRepository;
import com.jfra_13.grupos_electrogenos.repository.GrupoElectrogenoRepository;
import com.jfra_13.grupos_electrogenos.repository.SolicitudCompraRepository;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
import com.jfra_13.grupos_electrogenos.service.SolicitudCompraService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SolicitudCompraServiceImpl implements SolicitudCompraService {

    private final SolicitudCompraRepository repository;
    private final GrupoElectrogenoService grupoService;
    private final EntidadRepository entidadRepository;
    private final GrupoElectrogenoRepository grupoRepository;

    public SolicitudCompraServiceImpl(SolicitudCompraRepository repository, 
                                      GrupoElectrogenoService grupoService, 
                                      EntidadRepository entidadRepository,
                                      GrupoElectrogenoRepository grupoRepository) {
        this.repository = repository;
        this.grupoService = grupoService;
        this.entidadRepository = entidadRepository;
        this.grupoRepository = grupoRepository;
    }

    @Override
    @Transactional
    public SolicitudCompraResponseDTO crearSolicitud(SolicitudCompraRequestDTO dto) {
        Entidad entidad = entidadRepository.findById(dto.getEntidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Entidad no encontrada"));

        List<GrupoElectrogeno> candidatos = grupoRepository.findByTipoCombustibleOrderByPMaxDesc(dto.getTipoCombustible());
        
        GrupoElectrogeno grupoSeleccionado = candidatos.stream()
                .filter(g -> g.getPMax() >= dto.getPotenciaRequerida())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un Grupo Electrógeno que cumpla con la potencia requerida para este combustible"));

        SolicitudCompra solicitud = new SolicitudCompra();
        solicitud.setIdentificador(UUID.randomUUID().toString().substring(0, 8));
        solicitud.setNombreSolicitante(dto.getNombreSolicitante());
        solicitud.setTipoPago(dto.getTipoPago());
        solicitud.setCantidad(dto.getCantidad());
        solicitud.setPotenciaRequerida(dto.getPotenciaRequerida());
        solicitud.setTipoCombustible(dto.getTipoCombustible());
        solicitud.setVidaUtilSolicitada(dto.getVidaUtilSolicitada());
        solicitud.setEntidad(entidad);
        solicitud.setGrupoElectrogeno(grupoSeleccionado);

        SolicitudCompra guardada = repository.save(solicitud);
        return mapToResponseDTO(guardada);
    }

    @Override
    public SolicitudCompraResponseDTO obtenerPorId(Long id) {
        SolicitudCompra entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de compra no encontrada"));
        return mapToResponseDTO(entidad);
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
            
            List<GrupoElectrogeno> candidatos = grupoRepository.findByTipoCombustibleOrderByPMaxDesc(dto.getTipoCombustible());
            GrupoElectrogeno nuevoGrupo = candidatos.stream()
                .filter(g -> g.getPMax() >= dto.getPotenciaRequerida())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un Grupo Electrógeno que cumpla con la nueva potencia requerida para este combustible"));
            
            existing.setGrupoElectrogeno(nuevoGrupo);
        }

        existing.setNombreSolicitante(dto.getNombreSolicitante());
        existing.setTipoPago(dto.getTipoPago());
        existing.setCantidad(dto.getCantidad());
        existing.setPotenciaRequerida(dto.getPotenciaRequerida());
        existing.setTipoCombustible(dto.getTipoCombustible());
        existing.setVidaUtilSolicitada(dto.getVidaUtilSolicitada());
        existing.setEntidad(entidad);
        
        SolicitudCompra actualizada = repository.save(existing);
        return mapToResponseDTO(actualizada);
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

    private SolicitudCompraResponseDTO mapToResponseDTO(SolicitudCompra entidad) {
        return SolicitudCompraResponseDTO.builder()
                .id(entidad.getId())
                .identificador(entidad.getIdentificador())
                .nombreSolicitante(entidad.getNombreSolicitante())
                .tipoPago(entidad.getTipoPago())
                .cantidad(entidad.getCantidad())
                .potenciaRequerida(entidad.getPotenciaRequerida())
                .tipoCombustible(entidad.getTipoCombustible())
                .vidaUtilSolicitada(entidad.getVidaUtilSolicitada())
                .entidadId(entidad.getEntidad().getId())
                .entidadNombre(entidad.getEntidad().getNombre())
                .grupoId(entidad.getGrupoElectrogeno().getId())
                .grupoCodigo(entidad.getGrupoElectrogeno().getCodigo())
                .precioVentaUnitario(grupoService.calcularPrecioVenta(entidad.getGrupoElectrogeno()))
                .build();
    }
}
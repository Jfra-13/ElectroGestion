package com.jfra_13.grupos_electrogenos.service.impl;

import com.jfra_13.grupos_electrogenos.exception.ResourceNotFoundException;
import com.jfra_13.grupos_electrogenos.model.dto.RankingEntidadDTO;
import com.jfra_13.grupos_electrogenos.model.dto.ReportePagoDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.entity.Entidad;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.SolicitudCompra;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import com.jfra_13.grupos_electrogenos.repository.EntidadRepository;
import com.jfra_13.grupos_electrogenos.repository.SolicitudCompraRepository;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
import com.jfra_13.grupos_electrogenos.service.SolicitudCompraService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SolicitudCompraServiceImpl implements SolicitudCompraService {

    private final SolicitudCompraRepository repository;
    private final GrupoElectrogenoService grupoService; // Usamos el cerebro de cotizaciones
    private final EntidadRepository entidadRepository;

    public SolicitudCompraServiceImpl(SolicitudCompraRepository repository, GrupoElectrogenoService grupoService, EntidadRepository entidadRepository) {
        this.repository = repository;
        this.grupoService = grupoService;
        this.entidadRepository = entidadRepository;
    }

    @Override
    @Transactional
    public SolicitudCompra crearSolicitud(SolicitudCompraRequestDTO dto) {
        Entidad entidad = entidadRepository.findById(dto.getEntidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Entidad no encontrada"));

        // Lógica simple para asignar un grupo: buscar uno que coincida con el combustible y tenga potencia suficiente
        // En un caso real esto sería más complejo. Aquí buscaremos uno que cumpla los requisitos mínimos.
        List<GrupoElectrogeno> candidatos = grupoService.buscarPorCombustible(dto.getTipoCombustible());
        
        GrupoElectrogeno grupoSeleccionado = candidatos.stream()
                .filter(g -> g.getPMax() >= dto.getPotenciaRequerida())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un Grupo Electrógeno que cumpla con la potencia requerida para este combustible"));

        SolicitudCompra solicitud = new SolicitudCompra();
        solicitud.setIdentificador(UUID.randomUUID().toString().substring(0, 8)); // Generar ID corto
        solicitud.setNombreSolicitante(dto.getNombreSolicitante());
        solicitud.setTipoPago(dto.getTipoPago());
        solicitud.setCantidad(dto.getCantidad());
        solicitud.setPotenciaRequerida(dto.getPotenciaRequerida());
        solicitud.setTipoCombustible(dto.getTipoCombustible());
        solicitud.setVidaUtilSolicitada(dto.getVidaUtilSolicitada());
        solicitud.setEntidad(entidad);
        solicitud.setGrupoElectrogeno(grupoSeleccionado);

        return repository.save(solicitud);
    }

    @Override
    public SolicitudCompra obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de compra no encontrada"));
    }

    @Override
    @Transactional
    public SolicitudCompra actualizarSolicitud(Long id, SolicitudCompraRequestDTO dto) {
        SolicitudCompra existing = obtenerPorId(id);
        
        Entidad entidad = entidadRepository.findById(dto.getEntidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Entidad no encontrada"));

        existing.setNombreSolicitante(dto.getNombreSolicitante());
        existing.setTipoPago(dto.getTipoPago());
        existing.setCantidad(dto.getCantidad());
        existing.setPotenciaRequerida(dto.getPotenciaRequerida());
        existing.setTipoCombustible(dto.getTipoCombustible());
        existing.setVidaUtilSolicitada(dto.getVidaUtilSolicitada());
        existing.setEntidad(entidad);
        
        // Podríamos re-evaluar el grupo si el combustible o potencia cambian, 
        // pero por simplicidad mantendremos el flujo de actualización de datos básicos.

        return repository.save(existing);
    }

    @Override
    @Transactional
    public void eliminarSolicitud(Long id) {
        SolicitudCompra existing = obtenerPorId(id);
        repository.delete(existing);
    }

    @Override
    public List<RankingEntidadDTO> obtenerRankingClientes() {
        return repository.obtenerRankingEntidades();
    }

    @Override
    public List<ReportePagoDTO> obtenerReportePorPago(TipoPago tipoPago) {
        return repository.obtenerReportePorTipoPago(tipoPago);
    }

    // RF06: La gran sumatoria financiera
    @Override
    public Double calcularIngresosTotales() {
        List<SolicitudCompra> todasLasVentas = repository.findAll();
        double totalRecaudado = 0.0;

        for (SolicitudCompra venta : todasLasVentas) {
            // Calculamos el precio unitario usando el motor del Sprint 2
            double precioUnitario = grupoService.calcularPrecioVenta(venta.getGrupoElectrogeno());

            // Multiplicamos por la cantidad que se llevó en esa solicitud
            totalRecaudado += (precioUnitario * venta.getCantidad());
        }

        return totalRecaudado;
    }
}
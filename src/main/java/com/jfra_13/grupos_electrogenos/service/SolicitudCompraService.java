package com.jfra_13.grupos_electrogenos.service;

import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraResponseDTO;
import com.jfra_13.grupos_electrogenos.model.dto.RankingEntidadDTO;
import com.jfra_13.grupos_electrogenos.model.dto.ReportePagoDTO;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import java.util.List;
import org.springframework.data.domain.Pageable;
import com.jfra_13.grupos_electrogenos.model.dto.PaginatedResponseDTO;

public interface SolicitudCompraService {
    SolicitudCompraResponseDTO crearSolicitud(SolicitudCompraRequestDTO dto);
    SolicitudCompraResponseDTO obtenerPorId(Long id);
    SolicitudCompraResponseDTO actualizarSolicitud(Long id, SolicitudCompraRequestDTO dto);
    void eliminarSolicitud(Long id);

    List<RankingEntidadDTO> obtenerRankingClientes();
    List<ReportePagoDTO> obtenerReportePorPago(TipoPago tipoPago);
    Double calcularIngresosTotales(); // RF06

    // Listado paginado de ventas
    PaginatedResponseDTO<SolicitudCompraResponseDTO> listarVentasPaginado(Pageable pageable);
}
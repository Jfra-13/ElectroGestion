package com.jfra_13.grupos_electrogenos.service;

import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraResponseDTO;
import com.jfra_13.grupos_electrogenos.model.dto.RankingEntidadDTO;
import com.jfra_13.grupos_electrogenos.model.dto.RankingVendedorDTO;
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

    // Ranking de ventas por empleado (vista del jefe).
    List<RankingVendedorDTO> obtenerRankingVendedores();

    // Listado paginado de ventas. Un EMPLEADO ve solo las suyas; un ADMIN ve
    // todas, o las de un vendedor concreto si pasa vendedorId.
    PaginatedResponseDTO<SolicitudCompraResponseDTO> listarVentasPaginado(Pageable pageable, Long vendedorId);
}
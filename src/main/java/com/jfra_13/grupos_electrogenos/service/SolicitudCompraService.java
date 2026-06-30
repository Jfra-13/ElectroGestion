package com.jfra_13.grupos_electrogenos.service;

import com.jfra_13.grupos_electrogenos.model.dto.AnulacionRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraUpdateDTO;
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

    // Edición ACOTADA: solo nombreSolicitante. El resto es inmutable.
    SolicitudCompraResponseDTO actualizarSolicitud(Long id, SolicitudCompraUpdateDTO dto);

    // Anulación (reversa con rastro): marca ANULADA, repone stock, deja auditoría.
    SolicitudCompraResponseDTO anularVenta(Long id, AnulacionRequestDTO dto);

    List<RankingEntidadDTO> obtenerRankingClientes();
    List<ReportePagoDTO> obtenerReportePorPago(TipoPago tipoPago);
    Double calcularIngresosTotales(); // RF06

    // Ranking de ventas por empleado (vista del jefe).
    List<RankingVendedorDTO> obtenerRankingVendedores();

    // Listado paginado de ventas. Un EMPLEADO ve solo las suyas; un ADMIN ve
    // todas, o las de un vendedor concreto si pasa vendedorId.
    PaginatedResponseDTO<SolicitudCompraResponseDTO> listarVentasPaginado(Pageable pageable, Long vendedorId);
}
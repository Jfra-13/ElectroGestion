package com.jfra_13.grupos_electrogenos.service;

import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.RankingEntidadDTO;
import com.jfra_13.grupos_electrogenos.model.dto.ReportePagoDTO;
import com.jfra_13.grupos_electrogenos.model.entity.SolicitudCompra;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import java.util.List;

public interface SolicitudCompraService {
    SolicitudCompra crearSolicitud(SolicitudCompraRequestDTO dto);
    SolicitudCompra obtenerPorId(Long id);
    SolicitudCompra actualizarSolicitud(Long id, SolicitudCompraRequestDTO dto);
    void eliminarSolicitud(Long id);

    List<RankingEntidadDTO> obtenerRankingClientes();
    List<ReportePagoDTO> obtenerReportePorPago(TipoPago tipoPago);
    Double calcularIngresosTotales(); // RF06
}
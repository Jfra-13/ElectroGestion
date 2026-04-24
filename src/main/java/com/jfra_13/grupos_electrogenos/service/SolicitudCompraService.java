package com.jfra_13.grupos_electrogenos.service;

import com.jfra_13.grupos_electrogenos.model.dto.RankingEntidadDTO;
import com.jfra_13.grupos_electrogenos.model.dto.ReportePagoDTO;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import java.util.List;

public interface SolicitudCompraService {
    List<RankingEntidadDTO> obtenerRankingClientes();
    List<ReportePagoDTO> obtenerReportePorPago(TipoPago tipoPago);
    Double calcularIngresosTotales(); // RF06
}
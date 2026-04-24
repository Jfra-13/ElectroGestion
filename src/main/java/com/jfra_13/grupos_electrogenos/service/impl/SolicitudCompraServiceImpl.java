package com.jfra_13.grupos_electrogenos.service.impl;

import com.jfra_13.grupos_electrogenos.model.dto.RankingEntidadDTO;
import com.jfra_13.grupos_electrogenos.model.dto.ReportePagoDTO;
import com.jfra_13.grupos_electrogenos.model.entity.SolicitudCompra;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import com.jfra_13.grupos_electrogenos.repository.SolicitudCompraRepository;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
import com.jfra_13.grupos_electrogenos.service.SolicitudCompraService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SolicitudCompraServiceImpl implements SolicitudCompraService {

    private final SolicitudCompraRepository repository;
    private final GrupoElectrogenoService grupoService; // Usamos el cerebro de cotizaciones

    public SolicitudCompraServiceImpl(SolicitudCompraRepository repository, GrupoElectrogenoService grupoService) {
        this.repository = repository;
        this.grupoService = grupoService;
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
package com.jfra_13.grupos_electrogenos.controller;

import com.jfra_13.grupos_electrogenos.model.dto.RankingEntidadDTO;
import com.jfra_13.grupos_electrogenos.model.dto.ReportePagoDTO;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import com.jfra_13.grupos_electrogenos.service.SolicitudCompraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ventas")
public class SolicitudCompraController {

    private final SolicitudCompraService service;

    public SolicitudCompraController(SolicitudCompraService service) {
        this.service = service;
    }

    @GetMapping("/ranking-clientes")
    public ResponseEntity<List<RankingEntidadDTO>> getRankingClientes() {
        return ResponseEntity.ok(service.obtenerRankingClientes());
    }

    @GetMapping("/reporte-pagos")
    public ResponseEntity<List<ReportePagoDTO>> getReportePagos(@RequestParam TipoPago tipo) {
        return ResponseEntity.ok(service.obtenerReportePorPago(tipo));
    }

    @GetMapping("/ingresos-totales")
    public ResponseEntity<Map<String, Double>> getIngresosTotales() {
        Double total = service.calcularIngresosTotales();

        // Devolvemos un JSON limpio con la llave "total"
        Map<String, Double> response = new HashMap<>();
        response.put("totalRecaudado", total);

        return ResponseEntity.ok(response);
    }
}
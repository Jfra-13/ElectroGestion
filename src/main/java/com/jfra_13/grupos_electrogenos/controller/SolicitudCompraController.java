package com.jfra_13.grupos_electrogenos.controller;

import com.jfra_13.grupos_electrogenos.model.dto.RankingEntidadDTO;
import com.jfra_13.grupos_electrogenos.model.dto.RankingVendedorDTO;
import com.jfra_13.grupos_electrogenos.model.dto.ReportePagoDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraResponseDTO;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import com.jfra_13.grupos_electrogenos.service.SolicitudCompraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.jfra_13.grupos_electrogenos.model.dto.PaginatedResponseDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/v1/ventas")
@Tag(name = "Ventas (Solicitudes de Compra)", description = "Operaciones relacionadas con el proceso de venta y reportes financieros")
public class SolicitudCompraController {

    private final SolicitudCompraService service;

    public SolicitudCompraController(SolicitudCompraService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    @Operation(summary = "Crear solicitud de compra", description = "Registra una nueva venta. La venta queda atribuida al usuario autenticado. Requiere ROLE_ADMIN o ROLE_EMPLEADO.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Venta registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    public ResponseEntity<SolicitudCompraResponseDTO> crearSolicitud(@Valid @RequestBody SolicitudCompraRequestDTO dto) {
        return new ResponseEntity<>(service.crearSolicitud(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener venta por ID", description = "Devuelve los detalles de una solicitud de compra específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Venta encontrada"),
            @ApiResponse(responseCode = "404", description = "Venta no encontrada")
    })
    public ResponseEntity<SolicitudCompraResponseDTO> obtenerSolicitud(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar venta", description = "Modifica los datos de una venta existente. Requiere ROLE_ADMIN.")
    public ResponseEntity<SolicitudCompraResponseDTO> actualizarSolicitud(@PathVariable Long id, @Valid @RequestBody SolicitudCompraRequestDTO dto) {
        return ResponseEntity.ok(service.actualizarSolicitud(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar venta", description = "Borra una venta del sistema. Requiere ROLE_ADMIN.")
    public ResponseEntity<Void> eliminarSolicitud(@PathVariable Long id) {
        service.eliminarSolicitud(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ranking-clientes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ranking de clientes", description = "Lista los clientes con más compras realizadas. Requiere ROLE_ADMIN.")
    public ResponseEntity<List<RankingEntidadDTO>> getRankingClientes() {
        return ResponseEntity.ok(service.obtenerRankingClientes());
    }

    @GetMapping
    @Operation(summary = "Listar ventas paginado",
            description = "Un EMPLEADO ve solo sus ventas; un ADMIN ve todas, o las de un empleado puntual con vendedorId.")
    public ResponseEntity<PaginatedResponseDTO<SolicitudCompraResponseDTO>> listarVentas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "id,asc") String sort,
            @RequestParam(required = false) Long vendedorId) {

        Sort.Direction direction = sort.toLowerCase().endsWith("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProp = sort.contains(",") ? sort.split(",")[0] : sort;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProp));

        PaginatedResponseDTO<SolicitudCompraResponseDTO> result = service.listarVentasPaginado(pageable, vendedorId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/por-empleado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ranking de ventas por empleado",
            description = "Cantidad de ventas y total recaudado por cada vendedor. Vista del jefe. Requiere ROLE_ADMIN.")
    public ResponseEntity<List<RankingVendedorDTO>> getRankingPorEmpleado() {
        return ResponseEntity.ok(service.obtenerRankingVendedores());
    }

    @GetMapping("/reporte-pagos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reporte por tipo de pago", description = "Lista ventas filtradas por el método de pago. Requiere ROLE_ADMIN.")
    public ResponseEntity<List<ReportePagoDTO>> getReportePagos(@RequestParam TipoPago tipo) {
        return ResponseEntity.ok(service.obtenerReportePorPago(tipo));
    }

    @GetMapping("/ingresos-totales")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ingresos totales", description = "Calcula la suma total de todas las ventas. Requiere ROLE_ADMIN.")
    public ResponseEntity<Map<String, Double>> getIngresosTotales() {
        Double total = service.calcularIngresosTotales();

        // Devolvemos un JSON limpio con la llave "total"
        Map<String, Double> response = new HashMap<>();
        response.put("totalRecaudado", total);

        return ResponseEntity.ok(response);
    }
}
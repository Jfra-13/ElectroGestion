package com.jfra_13.grupos_electrogenos.controller;

import com.jfra_13.grupos_electrogenos.model.dto.EntidadRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.EntidadResponseDTO;
import com.jfra_13.grupos_electrogenos.service.EntidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestión de clientes (entidades). Da de alta y busca clientes para que el
 * formulario de venta pueda resolver un {@code entidadId} válido (alta inline).
 * Disponible para ADMIN y EMPLEADO.
 */
@RestController
@RequestMapping("/api/v1/clientes")
@Tag(name = "Clientes", description = "Alta y búsqueda de clientes. Requiere ROLE_ADMIN o ROLE_EMPLEADO.")
public class EntidadController {

    private final EntidadService entidadService;

    public EntidadController(EntidadService entidadService) {
        this.entidadService = entidadService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    @Operation(summary = "Crear cliente", description = "Crea un cliente nuevo. El nombre es único (case-insensitive).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Nombre vacío o inválido"),
            @ApiResponse(responseCode = "409", description = "Ya existe un cliente con ese nombre"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación")
    })
    public ResponseEntity<EntidadResponseDTO> crear(@Valid @RequestBody EntidadRequestDTO dto) {
        return new ResponseEntity<>(entidadService.crear(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    @Operation(summary = "Buscar clientes", description = "Lista clientes para autocomplete. Sin 'nombre' devuelve todos en orden alfabético.")
    public ResponseEntity<List<EntidadResponseDTO>> buscar(
            @RequestParam(value = "nombre", required = false) String nombre) {
        return ResponseEntity.ok(entidadService.buscar(nombre));
    }
}

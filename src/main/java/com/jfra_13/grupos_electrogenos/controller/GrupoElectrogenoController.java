package com.jfra_13.grupos_electrogenos.controller;

import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoResponseDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoMovilResumenDTO;
import com.jfra_13.grupos_electrogenos.model.dto.PaginatedResponseDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
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

@RestController
@RequestMapping("/api/v1/grupos-electrogenos")
@Tag(name = "Grupos Electrógenos", description = "Operaciones relacionadas con la gestión de grupos electrógenos")
public class GrupoElectrogenoController {

    private final GrupoElectrogenoService service;

    public GrupoElectrogenoController(GrupoElectrogenoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todos los grupos electrógenos", description = "Devuelve una lista paginada de todos los grupos electrógenos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    })
    public ResponseEntity<PaginatedResponseDTO<GrupoElectrogenoResponseDTO>> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "id,asc") String sort) {

        Sort.Direction direction = sort.toLowerCase().endsWith("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProp = sort.contains(",") ? sort.split(",")[0] : sort;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProp));

        PaginatedResponseDTO<GrupoElectrogenoResponseDTO> resultado = service.listarPaginado(pageable);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear grupo electrógeno", description = "Añade un nuevo grupo electrógeno al inventario. Requiere ROLE_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Grupo creado exitosamente"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    public ResponseEntity<GrupoElectrogenoResponseDTO> crearGrupo(@Valid @RequestBody GrupoElectrogenoRequestDTO dto) {
        GrupoElectrogenoResponseDTO nuevoGrupo = service.guardarGrupo(dto);
        return new ResponseEntity<>(nuevoGrupo, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener grupo por ID", description = "Devuelve los detalles de un grupo electrógeno específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grupo encontrado"),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<GrupoElectrogenoResponseDTO> obtenerGrupo(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar grupo electrógeno", description = "Modifica los datos de un grupo existente. Requiere ROLE_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grupo actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<GrupoElectrogenoResponseDTO> modificarGrupo(@PathVariable Long id, @Valid @RequestBody GrupoElectrogenoRequestDTO dto) {
        return ResponseEntity.ok(service.actualizarGrupo(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar grupo electrógeno", description = "Borra un grupo electrógeno del sistema. Requiere ROLE_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Grupo eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<Void> eliminarGrupo(@PathVariable Long id) {
        service.eliminarGrupo(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar stock", description = "Actualiza el stock disponible de un grupo. Requiere ROLE_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<GrupoElectrogenoResponseDTO> actualizarStock(
            @PathVariable Long id,
            @RequestParam Integer nuevoStock) {
        return ResponseEntity.ok(service.actualizarStock(id, nuevoStock));
    }

    @GetMapping("/{id}/precio")
    @Operation(summary = "Cotizar precio", description = "Calcula el precio de venta final de un grupo")
    public ResponseEntity<Double> cotizarPrecio(@PathVariable Long id) {
        GrupoElectrogenoResponseDTO grupo = service.obtenerPorId(id);
        // Nota: El service de negocio sigue exponiendo calcularPrecioVenta para uso interno
        // pero aquí podemos devolver el campo ya calculado en el DTO o llamar al service si se requiere frescura.
        return ResponseEntity.ok(grupo.getPrecioVentaCalculado());
    }

    @GetMapping("/filtro/combustible")
    @Operation(summary = "Filtrar por combustible", description = "Lista grupos electrógenos según el tipo de combustible")
    public ResponseEntity<PaginatedResponseDTO<GrupoElectrogenoResponseDTO>> filtrarPorCombustible(
            @RequestParam TipoCombustible tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "pMax,desc") String sort) {

        Sort.Direction direction = sort.toLowerCase().endsWith("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProp = sort.contains(",") ? sort.split(",")[0] : sort;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProp));

        PaginatedResponseDTO<GrupoElectrogenoResponseDTO> resultados = service.buscarPorCombustiblePaginado(tipo, pageable);
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/filtro/moviles-automaticos")
    @Operation(summary = "Filtrar móviles por material del eje", description = "Lista grupos móviles con tanque automático según material del eje")
    public ResponseEntity<PaginatedResponseDTO<GrupoMovilResumenDTO>> filtrarMoviles(
            @RequestParam MaterialEje materialEje,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "codigo,asc") String sort) {

        Sort.Direction direction = sort.toLowerCase().endsWith("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String sortProp = sort.contains(",") ? sort.split(",")[0] : sort;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProp));

        PaginatedResponseDTO<GrupoMovilResumenDTO> resultados = service.buscarMovilesPorEjePaginado(materialEje, pageable);
        return ResponseEntity.ok(resultados);
    }
}
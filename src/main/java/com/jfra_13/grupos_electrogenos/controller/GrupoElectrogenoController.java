package com.jfra_13.grupos_electrogenos.controller;

import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoResponseDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoMovilResumenDTO;
import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grupos-electrogenos")
public class GrupoElectrogenoController {

    private final GrupoElectrogenoService service;

    public GrupoElectrogenoController(GrupoElectrogenoService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GrupoElectrogenoResponseDTO> crearGrupo(@Valid @RequestBody GrupoElectrogenoRequestDTO dto) {
        GrupoElectrogenoResponseDTO nuevoGrupo = service.guardarGrupo(dto);
        return new ResponseEntity<>(nuevoGrupo, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrupoElectrogenoResponseDTO> obtenerGrupo(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GrupoElectrogenoResponseDTO> modificarGrupo(@PathVariable Long id, @Valid @RequestBody GrupoElectrogenoRequestDTO dto) {
        return ResponseEntity.ok(service.actualizarGrupo(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarGrupo(@PathVariable Long id) {
        service.eliminarGrupo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/precio")
    public ResponseEntity<Double> cotizarPrecio(@PathVariable Long id) {
        GrupoElectrogenoResponseDTO grupo = service.obtenerPorId(id);
        // Nota: El service de negocio sigue exponiendo calcularPrecioVenta para uso interno
        // pero aquí podemos devolver el campo ya calculado en el DTO o llamar al service si se requiere frescura.
        return ResponseEntity.ok(grupo.getPrecioVentaCalculado());
    }

    @GetMapping("/filtro/combustible")
    public ResponseEntity<List<GrupoElectrogenoResponseDTO>> filtrarPorCombustible(@RequestParam TipoCombustible tipo) {
        List<GrupoElectrogenoResponseDTO> resultados = service.buscarPorCombustible(tipo);
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/filtro/moviles-automaticos")
    public ResponseEntity<List<GrupoMovilResumenDTO>> filtrarMoviles(
            @RequestParam MaterialEje materialEje) {

        List<GrupoMovilResumenDTO> resultados = service.buscarMovilesPorEje(materialEje);
        return ResponseEntity.ok(resultados);
    }
}
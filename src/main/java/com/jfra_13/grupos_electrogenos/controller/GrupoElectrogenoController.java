package com.jfra_13.grupos_electrogenos.controller;

import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/grupos-electrogenos")
public class GrupoElectrogenoController {

    private final GrupoElectrogenoService service;

    public GrupoElectrogenoController(GrupoElectrogenoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<GrupoElectrogeno> crearGrupo(@RequestBody GrupoElectrogeno grupo) {
        // Nota: En un proyecto real estricto, aquí recibiríamos un RequestDTO y lo mapearíamos a Entity,
        // pero para mantener la agilidad del MVP, recibiremos la entidad directamente por ahora.
        GrupoElectrogeno nuevoGrupo = service.guardarGrupo(grupo);
        return new ResponseEntity<>(nuevoGrupo, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/precio")
    public ResponseEntity<Double> cotizarPrecio(@PathVariable Long id) {
        // Por ahora simularemos la obtención (esto lo mejoraremos en el Sprint 4 con busquedas reales)
        // La idea es que este endpoint use service.calcularPrecioVenta()
        return ResponseEntity.ok(0.0); // Placeholder
    }
}
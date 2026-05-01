package com.jfra_13.grupos_electrogenos.controller;

import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoResponseDTO;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/grupos-electrogenos")
public class GrupoElectrogenoController {

    private final GrupoElectrogenoService service;

    public GrupoElectrogenoController(GrupoElectrogenoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<GrupoElectrogeno> crearGrupo(@RequestBody GrupoElectrogeno grupo) {
        GrupoElectrogeno nuevoGrupo = service.guardarGrupo(grupo);
        return new ResponseEntity<>(nuevoGrupo, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrupoElectrogeno> obtenerGrupo(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GrupoElectrogeno> modificarGrupo(@PathVariable Long id, @RequestBody GrupoElectrogeno grupo) {
        return ResponseEntity.ok(service.actualizarGrupo(id, grupo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarGrupo(@PathVariable Long id) {
        service.eliminarGrupo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/precio")
    public ResponseEntity<Double> cotizarPrecio(@PathVariable Long id) {
        GrupoElectrogeno grupo = service.obtenerPorId(id);
        Double precio = service.calcularPrecioVenta(grupo);
        return ResponseEntity.ok(precio);
    }


    @GetMapping("/filtro/combustible")
    public ResponseEntity<List<GrupoElectrogeno>> filtrarPorCombustible(@RequestParam TipoCombustible tipo) {
        List<GrupoElectrogeno> resultados = service.buscarPorCombustible(tipo);
        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/filtro/moviles-automaticos")
    public ResponseEntity<List<GrupoElectrogenoResponseDTO>> filtrarMoviles(
            @RequestParam MaterialEje materialEje) {

        List<com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoResponseDTO> resultados =
                service.buscarMovilesPorEje(materialEje);
        return ResponseEntity.ok(resultados);
    }
}
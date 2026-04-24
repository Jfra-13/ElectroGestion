package com.jfra_13.grupos_electrogenos.service;

import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;

import java.util.List;

public interface GrupoElectrogenoService {
    // Método principal para nuestra regla de negocio
    Double calcularPrecioVenta(GrupoElectrogeno grupo);

    // Método para guardar usando el repositorio
    GrupoElectrogeno guardarGrupo(GrupoElectrogeno grupo);
    // Nuevos métodos para el Sprint 4
    List<GrupoElectrogeno> buscarPorCombustible(TipoCombustible combustible);

    // Devolvemos DTOs porque el PDF pide estrictamente "una lista con el código y la vida útil"
    List<com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoResponseDTO> buscarMovilesPorEje(MaterialEje material);
}
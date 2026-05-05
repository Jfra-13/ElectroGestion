package com.jfra_13.grupos_electrogenos.service;

import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoResponseDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoMovilResumenDTO;
import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;

import java.util.List;

public interface GrupoElectrogenoService {
    // Método principal para nuestra regla de negocio
    Double calcularPrecioVenta(com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno grupo);

    // Método para guardar usando el repositorio
    GrupoElectrogenoResponseDTO guardarGrupo(GrupoElectrogenoRequestDTO dto);

    // Métodos CRUD básicos para RF01
    GrupoElectrogenoResponseDTO obtenerPorId(Long id);
    GrupoElectrogenoResponseDTO actualizarGrupo(Long id, GrupoElectrogenoRequestDTO dto);
    void eliminarGrupo(Long id);

    // Nuevos métodos para el Sprint 4
    List<GrupoElectrogenoResponseDTO> buscarPorCombustible(TipoCombustible combustible);

    // Devolvemos DTOs específicos porque el RF07 pide estrictamente "una lista con el código y la vida útil"
    List<GrupoMovilResumenDTO> buscarMovilesPorEje(MaterialEje material);
}
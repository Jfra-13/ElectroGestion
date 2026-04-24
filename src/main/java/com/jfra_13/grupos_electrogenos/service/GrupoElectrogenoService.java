package com.jfra_13.grupos_electrogenos.service;

import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;

public interface GrupoElectrogenoService {
    // Método principal para nuestra regla de negocio
    Double calcularPrecioVenta(GrupoElectrogeno grupo);

    // Método para guardar usando el repositorio
    GrupoElectrogeno guardarGrupo(GrupoElectrogeno grupo);
}
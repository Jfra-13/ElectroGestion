package com.jfra_13.grupos_electrogenos.service;

import com.jfra_13.grupos_electrogenos.model.dto.EntidadRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.EntidadResponseDTO;

import java.util.List;

public interface EntidadService {

    /** Crea un cliente. Lanza conflicto si el nombre ya existe (case-insensitive). */
    EntidadResponseDTO crear(EntidadRequestDTO dto);

    /**
     * Lista clientes para autocomplete. Si {@code nombre} es null/vacío devuelve
     * todos en orden alfabético; si viene, filtra por coincidencia parcial.
     */
    List<EntidadResponseDTO> buscar(String nombre);
}

package com.jfra_13.grupos_electrogenos.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Alta de cliente (entidad). Solo requiere el nombre del cliente. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntidadRequestDTO {

    @NotBlank
    @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres.")
    private String nombre;
}

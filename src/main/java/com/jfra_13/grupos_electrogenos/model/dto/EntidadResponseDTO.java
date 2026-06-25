package com.jfra_13.grupos_electrogenos.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Vista pública de un cliente (entidad). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntidadResponseDTO {
    private Long id;
    private String nombre;
    private LocalDateTime createdAt;
}

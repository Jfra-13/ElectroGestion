package com.jfra_13.grupos_electrogenos.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrupoMovilResumenDTO {
    private String codigo;
    private Integer vidaUtil;
}

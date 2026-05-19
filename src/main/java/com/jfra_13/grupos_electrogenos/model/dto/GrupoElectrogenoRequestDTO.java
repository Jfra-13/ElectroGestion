package com.jfra_13.grupos_electrogenos.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrupoElectrogenoRequestDTO {

    @NotBlank(message = "El código es obligatorio")
    private String codigo;

    @NotNull(message = "La vida útil es obligatoria")
    @Min(value = 1, message = "La vida útil debe ser al menos 1 año")
    private Integer vidaUtil;

    @NotNull(message = "El tipo de combustible es obligatorio")
    private TipoCombustible tipoCombustible;

    @NotNull(message = "El tipo de arranque es obligatorio")
    private TipoArranque tipoArranque;

    @NotNull(message = "La potencia mínima es obligatoria")
    @Min(value = 0, message = "La potencia mínima no puede ser negativa")
    @JsonProperty("pMin")
    private Double pMin;

    @NotNull(message = "La potencia máxima es obligatoria")
    @Min(value = 1, message = "La potencia máxima debe ser al menos 1")
    @JsonProperty("pMax")
    private Double pMax;

    private Boolean insonorizado;
    private Boolean capo;

    // Campos para el caso de ser móvil
    private Integer cantidadRuedas;
    private MaterialEje materialEje;
    
    private Boolean esMovil;
}

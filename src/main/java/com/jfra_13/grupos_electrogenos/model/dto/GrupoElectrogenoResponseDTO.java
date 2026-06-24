package com.jfra_13.grupos_electrogenos.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrupoElectrogenoResponseDTO {
    private Long id;
    private String codigo;
    private Integer vidaUtil;
    private TipoCombustible tipoCombustible;
    private TipoArranque tipoArranque;
    @JsonProperty("pMin")
    private Double pMin;
    @JsonProperty("pMax")
    private Double pMax;
    private Boolean insonorizado;
    private Boolean capo;
    private Double potenciaMedia;
    private Double precioVentaCalculado;
    private String tipoGrupo; // "Fijo" o "Móvil"
    
    // Campos específicos para móviles si aplica
    private Integer cantidadRuedas;
    private MaterialEje materialEje;
    private Integer stock;
}
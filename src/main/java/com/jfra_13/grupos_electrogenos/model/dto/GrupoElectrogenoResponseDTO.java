package com.jfra_13.grupos_electrogenos.model.dto;

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
    private Double pMin;
    private Double pMax;
    private Boolean insonorizado;
    private Boolean capo;
    private Double potenciaMedia;
    private Double precioVentaCalculado;
    private String tipoGrupo; // "Fijo" o "Móvil"
    
    // Campos específicos para móviles si aplica
    private Integer cantidadRuedas;
    private MaterialEje materialEje;
}
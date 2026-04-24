package com.jfra_13.grupos_electrogenos.model.dto;

import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GrupoElectrogenoResponseDTO {
    private String codigo;
    private Integer vidaUtil;
    private TipoCombustible tipoCombustible;
    private TipoArranque tipoArranque;
    private Double potenciaMedia;
    private Double precioVentaCalculado;
    private String tipoGrupo; // "Fijo" o "Móvil"
}
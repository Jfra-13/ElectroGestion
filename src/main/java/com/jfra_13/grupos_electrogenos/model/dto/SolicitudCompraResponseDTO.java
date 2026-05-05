package com.jfra_13.grupos_electrogenos.model.dto;

import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCompraResponseDTO {
    private Long id;
    private String identificador;
    private String nombreSolicitante;
    private TipoPago tipoPago;
    private Integer cantidad;
    private Double potenciaRequerida;
    private TipoCombustible tipoCombustible;
    private Integer vidaUtilSolicitada;
    private Long entidadId;
    private String entidadNombre;
    private Long grupoId;
    private String grupoCodigo;
    private Double precioVentaUnitario;
}

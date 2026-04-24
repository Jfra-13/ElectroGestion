package com.jfra_13.grupos_electrogenos.model.dto;

import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SolicitudCompraRequestDTO {

    @NotBlank(message = "El nombre del solicitante es obligatorio")
    private String nombreSolicitante;

    @NotNull(message = "Debe especificar el tipo de pago")
    private TipoPago tipoPago;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "Debe solicitar al menos 1 unidad")
    private Integer cantidad;

    @NotNull(message = "La potencia requerida es obligatoria")
    private Double potenciaRequerida;

    @NotNull(message = "El tipo de combustible es obligatorio")
    private TipoCombustible tipoCombustible;

    @NotNull(message = "La vida útil solicitada es obligatoria")
    @Min(value = 1, message = "La vida útil debe ser mayor a 0")
    private Integer vidaUtilSolicitada;

    @NotNull(message = "El ID de la entidad es obligatorio")
    private Long entidadId;
}
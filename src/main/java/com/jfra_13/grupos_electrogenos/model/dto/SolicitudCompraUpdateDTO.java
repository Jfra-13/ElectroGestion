package com.jfra_13.grupos_electrogenos.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Edición ACOTADA de una venta: solo campos no-financieros.
 * tipoPago, cantidad, potencia, combustible, grupo, entidad y precio son
 * inmutables: si están mal, se anula la venta y se registra una nueva.
 */
@Data
public class SolicitudCompraUpdateDTO {

    @NotBlank(message = "El nombre del solicitante es obligatorio")
    private String nombreSolicitante;
}

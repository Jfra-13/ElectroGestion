package com.jfra_13.grupos_electrogenos.exception;

/**
 * Se lanza al operar sobre una venta ya anulada (re-anularla o editarla).
 * Representa un conflicto de negocio (HTTP 409): la venta no está en un estado
 * que admita la operación.
 */
public class VentaYaAnuladaException extends RuntimeException {

    public VentaYaAnuladaException(String message) {
        super(message);
    }
}

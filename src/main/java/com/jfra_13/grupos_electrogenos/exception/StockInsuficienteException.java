package com.jfra_13.grupos_electrogenos.exception;

/**
 * Se lanza cuando una venta solicita más unidades de las que hay en stock
 * del grupo electrógeno. Representa un conflicto de negocio (HTTP 409).
 */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String message) {
        super(message);
    }
}

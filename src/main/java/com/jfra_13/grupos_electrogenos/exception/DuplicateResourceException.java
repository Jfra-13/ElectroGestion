package com.jfra_13.grupos_electrogenos.exception;

/**
 * Se lanza al intentar crear un recurso que ya existe (p. ej. un cliente con un
 * nombre ya registrado). Representa un conflicto de negocio (HTTP 409).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}

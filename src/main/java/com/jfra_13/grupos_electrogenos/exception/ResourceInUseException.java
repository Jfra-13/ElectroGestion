package com.jfra_13.grupos_electrogenos.exception;

/**
 * Se lanza al intentar eliminar un recurso que está referenciado por otros
 * (p. ej. un grupo electrógeno con ventas asociadas). Representa un conflicto
 * de negocio (HTTP 409): el borrado físico violaría una restricción de FK.
 */
public class ResourceInUseException extends RuntimeException {

    public ResourceInUseException(String message) {
        super(message);
    }
}

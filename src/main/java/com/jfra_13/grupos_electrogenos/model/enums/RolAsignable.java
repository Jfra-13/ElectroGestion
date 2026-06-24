package com.jfra_13.grupos_electrogenos.model.enums;

/**
 * Roles que un administrador puede asignar al crear un usuario por el canal
 * protegido. NO incluye ROLE_USER a propósito: ese rol es para el registro
 * público y no debe otorgarse desde la gestión de empleados.
 */
public enum RolAsignable {
    EMPLEADO,
    ADMIN;

    /** Nombre del rol tal como está sembrado en la tabla {@code roles}. */
    public String roleName() {
        return "ROLE_" + name();
    }
}

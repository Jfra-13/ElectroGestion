package com.jfra_13.grupos_electrogenos.model.dto;

import com.jfra_13.grupos_electrogenos.model.enums.RolAsignable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Alta de usuario por un administrador. El rol se elige de {@link RolAsignable}
 * (EMPLEADO o ADMIN); cualquier otro valor es rechazado por la deserialización.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUsuarioRequestDTO {

    @NotBlank
    private String username;

    @NotBlank
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
    private String password;

    @NotBlank
    @Email
    private String email;

    @NotNull
    private RolAsignable rol;
}

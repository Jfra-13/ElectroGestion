package com.jfra_13.grupos_electrogenos.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/** Vista pública de un usuario. Nunca expone la contraseña. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
}

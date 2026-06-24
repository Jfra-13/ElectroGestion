package com.jfra_13.grupos_electrogenos.controller;

import com.jfra_13.grupos_electrogenos.model.dto.CreateUsuarioRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.UsuarioResponseDTO;
import com.jfra_13.grupos_electrogenos.model.entity.Role;
import com.jfra_13.grupos_electrogenos.model.entity.Usuario;
import com.jfra_13.grupos_electrogenos.repository.RoleRepository;
import com.jfra_13.grupos_electrogenos.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gestión de usuarios reservada al administrador (el "jefe"). Permite dar de
 * alta empleados/administradores por un canal protegido, a diferencia del
 * registro público de {@code /auth/register} que solo crea ROLE_USER.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios", description = "Alta y listado de empleados/administradores. Requiere ROLE_ADMIN.")
public class UserController {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UsuarioRepository usuarioRepository, RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    @Operation(summary = "Crear usuario", description = "Crea un empleado o administrador con el rol indicado. Requiere ROLE_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Usuario o email ya existe, o datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No autorizado (requiere ROLE_ADMIN)")
    })
    public ResponseEntity<UsuarioResponseDTO> crear(@Valid @RequestBody CreateUsuarioRequestDTO dto) {
        if (usuarioRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe.");
        }
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado.");
        }

        String roleName = dto.getRol().roleName();
        Role role = roleRepository.findByNombre(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " no está sembrado en el sistema."));

        Usuario usuario = Usuario.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .roles(Set.of(role))
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        return new ResponseEntity<>(toDto(guardado), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Lista todos los usuarios del sistema para la gestión de empleados. Requiere ROLE_ADMIN.")
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        List<UsuarioResponseDTO> usuarios = usuarioRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(usuarios);
    }

    private UsuarioResponseDTO toDto(Usuario u) {
        Set<String> roles = u.getRoles().stream().map(Role::getNombre).collect(Collectors.toSet());
        return new UsuarioResponseDTO(u.getId(), u.getUsername(), u.getEmail(), roles);
    }
}

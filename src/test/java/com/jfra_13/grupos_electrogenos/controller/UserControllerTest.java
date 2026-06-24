package com.jfra_13.grupos_electrogenos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfra_13.grupos_electrogenos.model.entity.Usuario;
import com.jfra_13.grupos_electrogenos.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de aceptación de la Fase 1: alta de empleados/administradores por el
 * canal protegido (solo ADMIN) y rechazo de accesos indebidos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Sin autenticación, crear usuario es rechazado")
    void crearSinTokenRechazado() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("nuevo", "nuevo@example.com", "EMPLEADO")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    @DisplayName("Un EMPLEADO no puede crear usuarios (403)")
    void empleadoNoPuedeCrear() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("nuevo", "nuevo@example.com", "EMPLEADO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("El ADMIN crea un empleado con ROLE_EMPLEADO")
    void adminCreaEmpleado() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("vendedor1", "vendedor1@example.com", "EMPLEADO")))
                .andExpect(status().isCreated());

        Usuario creado = usuarioRepository.findByUsername("vendedor1").orElseThrow();
        Set<String> roles = creado.getRoles().stream()
                .map(r -> r.getNombre())
                .collect(Collectors.toSet());
        assertEquals(Set.of("ROLE_EMPLEADO"), roles);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Crear con username duplicado responde 400")
    void usernameDuplicadoRechazado() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("repetido", "repetido@example.com", "EMPLEADO")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("repetido", "otro@example.com", "EMPLEADO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Un rol no asignable (USER) es rechazado por validación (400)")
    void rolNoAsignableRechazado() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("intruso", "intruso@example.com", "USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("El ADMIN puede listar usuarios")
    void adminListaUsuarios() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isOk());
    }

    private String payload(String username, String email, String rol) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", "secret123",
                "email", email,
                "rol", rol
        ));
    }
}

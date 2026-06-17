package com.jfra_13.grupos_electrogenos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfra_13.grupos_electrogenos.model.dto.RegisterRequestDTO;
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
 * Tests de aceptación de la Fase 1 del plan de producción.
 * Cubre I1 (sin escalada de privilegios en el registro) e I2 (datos de
 * negocio detrás de autenticación + rol).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeguridadAccesoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ----- I1: el registro público nunca otorga roles del cliente -----

    @Test
    @DisplayName("I1: registrarse pidiendo ROLE_ADMIN deja al usuario solo con ROLE_USER")
    void registroIgnoraRolesDelCliente() throws Exception {
        // El DTO ya no expone 'roles'; aunque el cliente lo inyecte en el JSON,
        // el backend debe ignorarlo y asignar siempre el rol mínimo.
        String payloadConRolInyectado = objectMapper.writeValueAsString(Map.of(
                "username", "atacante",
                "password", "secret123",
                "email", "atacante@example.com",
                "roles", new String[]{"ROLE_ADMIN"}
        ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadConRolInyectado))
                .andExpect(status().isOk());

        Usuario creado = usuarioRepository.findByUsername("atacante").orElseThrow();
        Set<String> roles = creado.getRoles().stream()
                .map(r -> r.getNombre())
                .collect(Collectors.toSet());

        assertEquals(Set.of("ROLE_USER"), roles,
                "El registro público no debe otorgar ROLE_ADMIN");
    }

    @Test
    @DisplayName("I1: el RegisterRequestDTO no expone el campo roles")
    void dtoRegistroNoExponeRoles() {
        boolean tieneCampoRoles = java.util.Arrays.stream(RegisterRequestDTO.class.getDeclaredFields())
                .anyMatch(f -> f.getName().equals("roles"));
        org.junit.jupiter.api.Assertions.assertFalse(tieneCampoRoles,
                "RegisterRequestDTO no debe declarar el campo 'roles'");
    }

    // ----- I2: datos financieros/clientes requieren auth + rol -----

    @Test
    @DisplayName("I2: GET ingresos-totales sin token es rechazado")
    void ingresosTotalesSinTokenRechazado() throws Exception {
        mockMvc.perform(get("/api/v1/ventas/ingresos-totales"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("I2: GET ingresos-totales con ROLE_ADMIN responde 200")
    void ingresosTotalesConAdminOk() throws Exception {
        mockMvc.perform(get("/api/v1/ventas/ingresos-totales"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("I2: un ROLE_USER no puede leer reportes financieros (403)")
    void reporteFinancieroNiegaUsuarioComun() throws Exception {
        mockMvc.perform(get("/api/v1/ventas/ingresos-totales"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("I2: el catálogo de productos sigue siendo de lectura pública")
    void catalogoPublicoSigueAccesible() throws Exception {
        mockMvc.perform(get("/api/v1/grupos-electrogenos"))
                .andExpect(status().isOk());
    }
}

package com.jfra_13.grupos_electrogenos.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Test preflight OPTIONS a /api/v1/ventas con origen permitido")
    void testPreflightOptionsConOrigenPermitido() throws Exception {
        mockMvc.perform(options("/api/v1/ventas")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS"));
    }

    @Test
    @DisplayName("Test preflight OPTIONS con origen no autorizado")
    void testPreflightOptionsConOrigenNoAutorizado() throws Exception {
        mockMvc.perform(options("/api/v1/ventas")
                        .header(HttpHeaders.ORIGIN, "http://malicious-site.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test GET con origen permitido")
    void testGetConOrigenPermitido() throws Exception {
        mockMvc.perform(get("/api/v1/ventas")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }

    @Test
    @DisplayName("Test POST sin token pero con origen permitido debe devolver 403 (o 401)")
    void testPostSinTokenConOrigenPermitido() throws Exception {
        mockMvc.perform(post("/api/v1/ventas")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isForbidden()) // Spring Security devuelve 403 por falta de CSRF o Auth
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }
}

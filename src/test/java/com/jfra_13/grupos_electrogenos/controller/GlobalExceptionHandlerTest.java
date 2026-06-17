package com.jfra_13.grupos_electrogenos.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jfra_13.grupos_electrogenos.exception.GlobalExceptionHandler;
import com.jfra_13.grupos_electrogenos.security.JwtUtil;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * I8 — Acceptance test for the generic exception handler.
 * An unmapped exception must produce a controlled 500 response with NO stack trace
 * in the body, while the full detail is logged on the server.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    private static final String INTERNAL_DETAIL = "NullPointerException en la fila secreta de la tabla interna";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GrupoElectrogenoService service;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger handlerLogger;

    @BeforeEach
    void attachLogAppender() {
        handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        handlerLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        handlerLogger.detachAppender(logAppender);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("I8: una excepción no mapeada devuelve 500 genérico sin stack trace y loguea el detalle")
    void excepcionNoMapeadaDevuelveErrorGenericoYLogueaDetalle() throws Exception {
        // Arrange: el servicio lanza una excepción NO mapeada por el handler
        when(service.obtenerPorId(1L))
                .thenThrow(new RuntimeException(INTERNAL_DETAIL));

        // Act
        MvcResult result = mockMvc.perform(get("/api/v1/grupos-electrogenos/1/precio"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(
                        "Ocurrió un error inesperado. Intente nuevamente o contacte al administrador."))
                .andReturn();

        // Assert: el body NO filtra el detalle interno ni el tipo de excepción
        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .doesNotContain(INTERNAL_DETAIL)
                .doesNotContain("RuntimeException")
                .doesNotContain("at com.jfra_13"); // ningún frame del stack trace

        // Assert: el detalle completo SÍ queda en el log del servidor
        boolean detalleLogueado = logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .anyMatch(event ->
                        event.getThrowableProxy() != null
                                && event.getThrowableProxy().getMessage().contains(INTERNAL_DETAIL));
        assertThat(detalleLogueado)
                .as("El detalle de la excepción debe registrarse en el servidor")
                .isTrue();
    }
}

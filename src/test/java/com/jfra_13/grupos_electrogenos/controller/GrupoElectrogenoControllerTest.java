package com.jfra_13.grupos_electrogenos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoResponseDTO;
import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.security.JwtUtil;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GrupoElectrogenoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GrupoElectrogenoService service;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Debe retornar 201 CREATED al guardar un grupo electrógeno exitosamente")
    void testCrearGrupo() throws Exception {
        // Arrange
        GrupoElectrogenoRequestDTO requestDTO = GrupoElectrogenoRequestDTO.builder()
                .codigo("FJO-001")
                .vidaUtil(10)
                .tipoCombustible(TipoCombustible.GASOIL)
                .tipoArranque(TipoArranque.AUTOMATICO)
                .pMin(100.0)
                .pMax(200.0)
                .esMovil(false)
                .build();

        GrupoElectrogenoResponseDTO responseDTO = GrupoElectrogenoResponseDTO.builder()
                .id(1L)
                .codigo("FJO-001")
                .vidaUtil(10)
                .tipoCombustible(TipoCombustible.GASOIL)
                .tipoArranque(TipoArranque.AUTOMATICO)
                .pMin(100.0)
                .pMax(200.0)
                .precioVentaCalculado(1500.0)
                .build();

        when(service.guardarGrupo(any(GrupoElectrogenoRequestDTO.class))).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/v1/grupos-electrogenos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.codigo").value("FJO-001"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Debe retornar el precio calculado correctamente")
    void testCotizarPrecio() throws Exception {
        // Arrange
        GrupoElectrogenoResponseDTO responseDTO = GrupoElectrogenoResponseDTO.builder()
                .id(1L)
                .precioVentaCalculado(1500.0)
                .build();
        when(service.obtenerPorId(1L)).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/grupos-electrogenos/1/precio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1500.0));
    }
}
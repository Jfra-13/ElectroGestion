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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GrupoElectrogenoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GrupoElectrogenoService service;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
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

    @Test
    @DisplayName("Debe retornar lista paginada de todos los grupos electrógenos")
    void testListarTodosGrupos() throws Exception {
        // Arrange
        GrupoElectrogenoResponseDTO grupo1 = GrupoElectrogenoResponseDTO.builder()
                .id(1L)
                .codigo("FJO-001")
                .vidaUtil(10)
                .tipoCombustible(TipoCombustible.GASOIL)
                .tipoArranque(TipoArranque.AUTOMATICO)
                .pMin(100.0)
                .pMax(200.0)
                .precioVentaCalculado(1500.0)
                .build();

        com.jfra_13.grupos_electrogenos.model.dto.PaginatedResponseDTO<GrupoElectrogenoResponseDTO> paginatedResponse =
                new com.jfra_13.grupos_electrogenos.model.dto.PaginatedResponseDTO<>(
                        java.util.Arrays.asList(grupo1),
                        0,
                        10,
                        1,
                        1
                );

        when(service.listarPaginado(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(paginatedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/grupos-electrogenos")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].codigo").value("FJO-001"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Debe actualizar el stock correctamente")
    void testActualizarStock() throws Exception {
        // Arrange
        GrupoElectrogenoResponseDTO responseDTO = GrupoElectrogenoResponseDTO.builder()
                .id(1L)
                .codigo("G-POW-500")
                .vidaUtil(10)
                .tipoCombustible(TipoCombustible.GASOIL)
                .tipoArranque(TipoArranque.AUTOMATICO)
                .pMin(100.0)
                .pMax(200.0)
                .stock(50)
                .precioVentaCalculado(1500.0)
                .build();

        when(service.actualizarStock(1L, 50)).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/grupos-electrogenos/1/stock")
                        .param("nuevoStock", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stock").value(50));
    }
}
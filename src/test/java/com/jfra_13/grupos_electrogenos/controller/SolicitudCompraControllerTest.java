package com.jfra_13.grupos_electrogenos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraResponseDTO;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import com.jfra_13.grupos_electrogenos.security.JwtUtil;
import com.jfra_13.grupos_electrogenos.service.SolicitudCompraService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SolicitudCompraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SolicitudCompraService service;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Debe retornar 201 CREATED al crear una solicitud exitosamente")
    void testCrearSolicitud() throws Exception {
        SolicitudCompraRequestDTO requestDTO = new SolicitudCompraRequestDTO();
        requestDTO.setEntidadId(1L);
        requestDTO.setNombreSolicitante("Juan");
        requestDTO.setCantidad(1);
        requestDTO.setPotenciaRequerida(100.0);
        requestDTO.setTipoCombustible(TipoCombustible.GASOIL);
        requestDTO.setTipoPago(TipoPago.EFECTIVO);
        requestDTO.setVidaUtilSolicitada(5);

        SolicitudCompraResponseDTO responseDTO = SolicitudCompraResponseDTO.builder()
                .id(1L)
                .identificador("ID-TEST")
                .build();

        when(service.crearSolicitud(any())).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.identificador").value("ID-TEST"));
    }

    @Test
    @WithMockUser
    @DisplayName("Debe retornar los ingresos totales")
    void testGetIngresosTotales() throws Exception {
        when(service.calcularIngresosTotales()).thenReturn(5000.0);

        mockMvc.perform(get("/api/v1/ventas/ingresos-totales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecaudado").value(5000.0));
    }
}

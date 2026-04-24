package com.jfra_13.grupos_electrogenos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GrupoElectrogenoController.class)
class GrupoElectrogenoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GrupoElectrogenoService service; // Simulamos la capa de servicio

    @Autowired
    private ObjectMapper objectMapper; // Para convertir objetos a JSON

    @Test
    @DisplayName("Debe retornar 201 CREATED al guardar un grupo electrógeno exitosamente")
    void testCrearGrupo() throws Exception {
        // Arrange: Preparamos la data simulada
        GrupoElectrogeno grupoMock = new GrupoElectrogeno();
        grupoMock.setId(1L);
        grupoMock.setCodigo("FJO-001");
        grupoMock.setVidaUtil(10);
        grupoMock.setTipoCombustible(TipoCombustible.GASOIL);
        grupoMock.setTipoArranque(TipoArranque.AUTOMATICO);
        grupoMock.setPMin(100.0);
        grupoMock.setPMax(200.0);

        // Le decimos a Mockito: "Cuando el controlador llame a guardarGrupo, devuelve grupoMock"
        when(service.guardarGrupo(any(GrupoElectrogeno.class))).thenReturn(grupoMock);

        // Act & Assert: Simulamos la llamada HTTP y verificamos la respuesta
        mockMvc.perform(post("/api/v1/grupos-electrogenos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grupoMock)))
                .andExpect(status().isCreated()) // Esperamos HTTP 201
                .andExpect(jsonPath("$.id").value(1)) // Validamos que el JSON de respuesta tenga el ID 1
                .andExpect(jsonPath("$.codigo").value("FJO-001"));
    }
}
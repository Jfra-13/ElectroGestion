package com.jfra_13.grupos_electrogenos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import com.jfra_13.grupos_electrogenos.repository.EntidadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class FlujoCompraIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntidadRepository entidadRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void flujoCompletoCompra() throws Exception {
        // 1. Usar la entidad semilla fija
        assertTrue(entidadRepository.existsById(1L), "Debe existir la entidad semilla con ID 1");

        // 2. Crear Grupo Electrógeno
        GrupoElectrogenoRequestDTO grupoDTO = GrupoElectrogenoRequestDTO.builder()
                .codigo("INT-001")
                .vidaUtil(10)
                .tipoCombustible(TipoCombustible.GASOIL)
                .tipoArranque(TipoArranque.AUTOMATICO)
                .pMin(100.0)
                .pMax(500.0)
                .esMovil(false)
                .build();

        mockMvc.perform(post("/api/v1/grupos-electrogenos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grupoDTO)))
                .andExpect(status().isCreated());

        // 3. Crear Solicitud de Compra
        SolicitudCompraRequestDTO solicitudDTO = new SolicitudCompraRequestDTO();
        solicitudDTO.setEntidadId(1L);
        solicitudDTO.setNombreSolicitante("Comprador Test");
        solicitudDTO.setCantidad(2);
        solicitudDTO.setPotenciaRequerida(300.0);
        solicitudDTO.setTipoCombustible(TipoCombustible.GASOIL);
        solicitudDTO.setTipoPago(TipoPago.EFECTIVO);
        solicitudDTO.setVidaUtilSolicitada(5);

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitudDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grupoCodigo").value("INT-001"));

        // 4. Verificar Ingresos Totales
        mockMvc.perform(get("/api/v1/ventas/ingresos-totales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecaudado").isNumber());
    }
}

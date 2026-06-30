package com.jfra_13.grupos_electrogenos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de aceptación de la venta por grupo elegido (campo opcional grupoCodigo).
 * Cuando viene grupoCodigo se vende ESE grupo exacto (sin tasación): se valida su
 * stock, se congela su precio y los errores nombran el grupo elegido.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class VentaPorGrupoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private void crearGrupo(String codigo, double pMin, double pMax, int stock) throws Exception {
        GrupoElectrogenoRequestDTO grupo = GrupoElectrogenoRequestDTO.builder()
                .codigo(codigo)
                .vidaUtil(10)
                .tipoCombustible(TipoCombustible.NAFTA)
                .tipoArranque(TipoArranque.MANUAL)
                .pMin(pMin)
                .pMax(pMax)
                .esMovil(false)
                .stock(stock)
                .build();

        mockMvc.perform(post("/api/v1/grupos-electrogenos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grupo)))
                .andExpect(status().isCreated());
    }

    private SolicitudCompraRequestDTO ventaDTO(String grupoCodigo, int cantidad, double potencia) {
        SolicitudCompraRequestDTO dto = new SolicitudCompraRequestDTO();
        dto.setEntidadId(1L); // entidad semilla
        dto.setNombreSolicitante("Comprador Test");
        dto.setCantidad(cantidad);
        dto.setPotenciaRequerida(potencia);
        dto.setTipoCombustible(TipoCombustible.NAFTA);
        dto.setTipoPago(TipoPago.EFECTIVO);
        dto.setVidaUtilSolicitada(10);
        dto.setGrupoCodigo(grupoCodigo);
        return dto;
    }

    @Test
    @DisplayName("Venta por grupo feliz: vende el grupo elegido y descuenta su stock")
    void ventaPorGrupoFeliz() throws Exception {
        crearGrupo("GE-FIJ-PG1", 2.0, 8.0, 20);

        MvcResult res = mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaDTO("GE-FIJ-PG1", 5, 5.0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grupoCodigo").value("GE-FIJ-PG1"))
                .andReturn();

        JsonNode venta = objectMapper.readTree(res.getResponse().getContentAsString());
        double unitario = venta.get("precioVentaUnitario").asDouble();
        Assertions.assertEquals(unitario * 5, venta.get("total").asDouble(), 0.0001,
                "total debe ser unitario * cantidad del grupo elegido");
    }

    @Test
    @DisplayName("El bug eliminado: vende el grupo elegido aunque otro más barato cumpla los criterios")
    void vendeElGrupoElegidoNoElDeLaTasacion() throws Exception {
        // Ambos cumplen NAFTA potencia 5. La tasación (OrderByPMaxDesc) elegiría GE-MOV-PG2 (pMax mayor),
        // pero al mandar grupoCodigo debe venderse GE-FIJ-PG2.
        crearGrupo("GE-FIJ-PG2", 2.0, 8.0, 20);
        crearGrupo("GE-MOV-PG2", 4.0, 15.0, 0);

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaDTO("GE-FIJ-PG2", 1, 5.0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grupoCodigo").value("GE-FIJ-PG2"));
    }

    @Test
    @DisplayName("Stock insuficiente del elegido: 409 que nombra el grupo elegido")
    void stockInsuficienteNombraElGrupoElegido() throws Exception {
        crearGrupo("GE-MOV-PG3", 4.0, 15.0, 0); // stock 0

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaDTO("GE-MOV-PG3", 1, 5.0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("GE-MOV-PG3")));
    }

    @Test
    @DisplayName("Código inexistente: 404")
    void codigoInexistente() throws Exception {
        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaDTO("GE-XXX-999", 1, 5.0))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sin grupoCodigo: la tasación sigue intacta (regresión)")
    void sinGrupoCodigoCorreTasacion() throws Exception {
        crearGrupo("GE-FIJ-PG5", 2.0, 999999.0, 10); // pMax alto -> lo elige la tasación

        MvcResult res = mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaDTO(null, 1, 5.0))))
                .andExpect(status().isCreated())
                .andReturn();

        // Sin grupoCodigo se resuelve por tasación; debe asignar un grupo NAFTA válido.
        JsonNode venta = objectMapper.readTree(res.getResponse().getContentAsString());
        Assertions.assertNotNull(venta.get("grupoCodigo").asText());
    }
}

package com.jfra_13.grupos_electrogenos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de aceptación de la Fase 3.
 * I4: el precio/total de una venta quedan congelados (editar el grupo no altera
 * la recaudación histórica). I5: el stock se valida y descuenta con la venta.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class IntegridadVentaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long crearGrupo(String codigo, int vidaUtil, double pMin, double pMax, int stock) throws Exception {
        GrupoElectrogenoRequestDTO grupo = GrupoElectrogenoRequestDTO.builder()
                .codigo(codigo)
                .vidaUtil(vidaUtil)
                .tipoCombustible(TipoCombustible.GASOIL)
                .tipoArranque(TipoArranque.MANUAL)
                .pMin(pMin)
                .pMax(pMax)
                .esMovil(false)
                .stock(stock)
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/grupos-electrogenos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grupo)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    private SolicitudCompraRequestDTO ventaDTO(int cantidad, double potencia) {
        SolicitudCompraRequestDTO dto = new SolicitudCompraRequestDTO();
        dto.setEntidadId(1L); // entidad semilla
        dto.setNombreSolicitante("Comprador Test");
        dto.setCantidad(cantidad);
        dto.setPotenciaRequerida(potencia);
        dto.setTipoCombustible(TipoCombustible.GASOIL);
        dto.setTipoPago(TipoPago.EFECTIVO);
        dto.setVidaUtilSolicitada(5);
        return dto;
    }

    @Test
    @DisplayName("I4: editar el precio del grupo NO cambia el total/unitario de una venta ya hecha")
    void precioVentaQuedaCongelado() throws Exception {
        // pMax altísimo: garantiza que el selector (OrderByPMaxDesc) elija ESTE grupo,
        // sin depender de otros grupos que puedan existir en la base de tests.
        Long grupoId = crearGrupo("FROZEN-1", 10, 100.0, 999999.0, 10);

        // Venta de 2 unidades. Capturamos el precio/total congelados del response.
        MvcResult ventaRes = mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaDTO(2, 300.0))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode venta = objectMapper.readTree(ventaRes.getResponse().getContentAsString());
        long ventaId = venta.get("id").asLong();
        double unitarioCongelado = venta.get("precioVentaUnitario").asDouble();
        double totalCongelado = venta.get("total").asDouble();
        org.junit.jupiter.api.Assertions.assertEquals(unitarioCongelado * 2, totalCongelado, 0.0001,
                "total debe ser unitario * cantidad");

        // Recaudación antes de tocar el grupo.
        double ingresosAntes = leerIngresos();

        // Cambiar el precio del grupo (pMin 100 -> 500 => su precio calculado cambia).
        GrupoElectrogenoRequestDTO update = GrupoElectrogenoRequestDTO.builder()
                .codigo("FROZEN-1")
                .vidaUtil(20)
                .tipoCombustible(TipoCombustible.GASOIL)
                .tipoArranque(TipoArranque.MANUAL)
                .pMin(500.0)
                .pMax(999999.0)
                .esMovil(false)
                .build();
        mockMvc.perform(put("/api/v1/grupos-electrogenos/" + grupoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        // La venta sigue valiendo EXACTAMENTE lo mismo (precio congelado).
        mockMvc.perform(get("/api/v1/ventas/" + ventaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioVentaUnitario").value(unitarioCongelado))
                .andExpect(jsonPath("$.total").value(totalCongelado));

        // Y la recaudación total no cambió por editar el grupo.
        org.junit.jupiter.api.Assertions.assertEquals(ingresosAntes, leerIngresos(), 0.0001,
                "editar el grupo no debe alterar la recaudación histórica");
    }

    private double leerIngresos() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/ventas/ingresos-totales"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("totalRecaudado").asDouble();
    }

    @Test
    @DisplayName("I5: vender más que el stock disponible es rechazado (409)")
    void sobreventaRechazada() throws Exception {
        crearGrupo("STOCK-1", 10, 100.0, 999999.0, 2); // stock 2, pMax alto -> siempre seleccionado

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaDTO(3, 300.0)))) // pide 3
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("I5: una venta válida descuenta el stock por la cantidad vendida")
    void ventaValidaDescuentaStock() throws Exception {
        Long grupoId = crearGrupo("STOCK-2", 10, 100.0, 999999.0, 5); // stock 5, pMax alto -> siempre seleccionado

        mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaDTO(2, 300.0)))) // vende 2
                .andExpect(status().isCreated());

        MvcResult grupoRes = mockMvc.perform(get("/api/v1/grupos-electrogenos/" + grupoId))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode grupo = objectMapper.readTree(grupoRes.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(3, grupo.get("stock").asInt(),
                "El stock debe quedar en 5 - 2 = 3");
    }
}

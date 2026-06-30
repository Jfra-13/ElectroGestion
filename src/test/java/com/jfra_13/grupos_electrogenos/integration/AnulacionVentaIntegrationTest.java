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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parte 5: anulación de ventas (reversa con rastro) y PUT acotado.
 * Una venta no se borra ni se edita libre: se anula (repone stock, deja auditoría,
 * sale de los reportes) y solo se puede editar su nombreSolicitante.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class AnulacionVentaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Anular repone el stock y deja la venta ANULADA con auditoría")
    void anularReponeStockYDejaRastro() throws Exception {
        Long grupoId = crearGrupo("ANUL-1", 10);
        JsonNode venta = crearVenta(3, 300.0, "Comprador A");
        long ventaId = venta.get("id").asLong();

        // Tras vender 3 de 10, el stock quedó en 7.
        assertEquals(7, leerStock(grupoId), "stock tras la venta");

        anular(ventaId, "Error de carga")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADA"))
                .andExpect(jsonPath("$.motivoAnulacion").value("Error de carga"))
                .andExpect(jsonPath("$.anuladaPor").value("admin"))
                .andExpect(jsonPath("$.anuladaAt").isNotEmpty());

        // El stock vuelve al valor inicial: la anulación repone lo vendido.
        assertEquals(10, leerStock(grupoId), "stock repuesto tras anular");
    }

    @Test
    @DisplayName("Anular una venta ya anulada devuelve 409")
    void dobleAnulacionRechazada() throws Exception {
        crearGrupo("ANUL-2", 5);
        long ventaId = crearVenta(1, 300.0, "Comprador B").get("id").asLong();

        anular(ventaId, "Primera").andExpect(status().isOk());
        anular(ventaId, "Segunda").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Anular saca la venta de los ingresos totales")
    void anularSacaDeIngresos() throws Exception {
        crearGrupo("ANUL-3", 10);
        JsonNode venta = crearVenta(2, 300.0, "Comprador C");
        long ventaId = venta.get("id").asLong();
        double total = venta.get("total").asDouble();

        double ingresosConVenta = leerIngresos();
        anular(ventaId, "Anula ingresos").andExpect(status().isOk());
        double ingresosSinVenta = leerIngresos();

        assertEquals(ingresosConVenta - total, ingresosSinVenta, 0.0001,
                "los ingresos deben bajar exactamente el total de la venta anulada");
    }

    @Test
    @DisplayName("Anular sin rol ADMIN devuelve 403")
    void anularComoNoAdminRechazado() throws Exception {
        crearGrupo("ANUL-4", 5);
        long ventaId = crearVenta(1, 300.0, "Comprador D").get("id").asLong();

        mockMvc.perform(post("/api/v1/ventas/" + ventaId + "/anulacion")
                        .with(user("emp1").roles("EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"sin permiso\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT edita solo nombreSolicitante; tipoPago/cantidad/total/stock quedan intactos")
    void putSoloTocaNombreSolicitante() throws Exception {
        Long grupoId = crearGrupo("ANUL-5", 10);
        JsonNode venta = crearVenta(2, 300.0, "Nombre Viejo");
        long ventaId = venta.get("id").asLong();
        double totalOriginal = venta.get("total").asDouble();
        double unitarioOriginal = venta.get("precioVentaUnitario").asDouble();
        int stockTrasVenta = leerStock(grupoId);

        // Mandamos tipoPago y cantidad distintos a propósito: deben ser ignorados.
        mockMvc.perform(put("/api/v1/ventas/" + ventaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreSolicitante\":\"Nombre Nuevo\",\"tipoPago\":\"CHEQUE\",\"cantidad\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreSolicitante").value("Nombre Nuevo"))
                .andExpect(jsonPath("$.tipoPago").value("EFECTIVO"))
                .andExpect(jsonPath("$.cantidad").value(2))
                .andExpect(jsonPath("$.total").value(totalOriginal))
                .andExpect(jsonPath("$.precioVentaUnitario").value(unitarioOriginal));

        // El stock no se tocó: el PUT no mueve inventario.
        assertEquals(stockTrasVenta, leerStock(grupoId), "el PUT no debe alterar el stock");
    }

    @Test
    @DisplayName("PUT sobre una venta anulada devuelve 409")
    void putSobreAnuladaRechazado() throws Exception {
        crearGrupo("ANUL-6", 5);
        long ventaId = crearVenta(1, 300.0, "Comprador F").get("id").asLong();
        anular(ventaId, "Anula antes del PUT").andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/ventas/" + ventaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreSolicitante\":\"Intento\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("El listado incluye las anuladas; los reportes no")
    void listadoIncluyeAnuladasReportesNo() throws Exception {
        crearGrupo("ANUL-7", 5);
        long ventaId = crearVenta(1, 300.0, "ANUL-LISTADO").get("id").asLong();
        anular(ventaId, "Anula para listado").andExpect(status().isOk());

        // El listado SÍ muestra la anulada, con su estado, para que el front la pinte.
        MvcResult listado = mockMvc.perform(get("/api/v1/ventas"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(listado.getResponse().getContentAsString()).get("content");
        boolean anuladaEnListado = false;
        for (JsonNode v : content) {
            if (v.get("id").asLong() == ventaId) {
                assertEquals("ANULADA", v.get("estado").asText(), "la venta debe figurar ANULADA en el listado");
                anuladaEnListado = true;
            }
        }
        assertTrue(anuladaEnListado, "la venta anulada debe aparecer en el listado");

        // El reporte por pago NO la cuenta: filtra solo ACTIVA.
        MvcResult reporte = mockMvc.perform(get("/api/v1/ventas/reporte-pagos").param("tipo", "EFECTIVO"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode filas = objectMapper.readTree(reporte.getResponse().getContentAsString());
        boolean presenteEnReporte = false;
        for (JsonNode fila : filas) {
            if ("ANUL-LISTADO".equals(fila.get("solicitante").asText())) {
                presenteEnReporte = true;
            }
        }
        assertFalse(presenteEnReporte, "la venta anulada no debe aparecer en el reporte por pago");
    }

    // ----- helpers -----

    private Long crearGrupo(String codigo, int stock) throws Exception {
        GrupoElectrogenoRequestDTO grupo = GrupoElectrogenoRequestDTO.builder()
                .codigo(codigo)
                .vidaUtil(10)
                .tipoCombustible(TipoCombustible.GASOIL)
                .tipoArranque(TipoArranque.MANUAL)
                .pMin(100.0)
                .pMax(999999.0) // pMax alto: siempre seleccionado por el OrderByPMaxDesc
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

    private JsonNode crearVenta(int cantidad, double potencia, String nombre) throws Exception {
        SolicitudCompraRequestDTO dto = new SolicitudCompraRequestDTO();
        dto.setEntidadId(1L); // entidad semilla
        dto.setNombreSolicitante(nombre);
        dto.setCantidad(cantidad);
        dto.setPotenciaRequerida(potencia);
        dto.setTipoCombustible(TipoCombustible.GASOIL);
        dto.setTipoPago(TipoPago.EFECTIVO);
        dto.setVidaUtilSolicitada(5);

        MvcResult res = mockMvc.perform(post("/api/v1/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString());
    }

    private org.springframework.test.web.servlet.ResultActions anular(long ventaId, String motivo) throws Exception {
        return mockMvc.perform(post("/api/v1/ventas/" + ventaId + "/anulacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"" + motivo + "\"}"));
    }

    private int leerStock(Long grupoId) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/grupos-electrogenos/" + grupoId))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("stock").asInt();
    }

    private double leerIngresos() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/ventas/ingresos-totales"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("totalRecaudado").asDouble();
    }
}

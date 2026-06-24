package com.jfra_13.grupos_electrogenos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.entity.Role;
import com.jfra_13.grupos_electrogenos.model.entity.Usuario;
import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import com.jfra_13.grupos_electrogenos.repository.RoleRepository;
import com.jfra_13.grupos_electrogenos.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de aceptación de la Fase 3: permisos y filtrado por dueño.
 * El empleado crea y ve solo SUS ventas; el jefe (ADMIN) ve todas, filtra por
 * empleado y accede al ranking. Los reportes financieros siguen solo para ADMIN.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VentasPermisosIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long empleadoId;

    @BeforeEach
    void seedEmpleado() {
        Usuario emp = usuarioRepository.findByUsername("emp1").orElseGet(() -> {
            Role rolEmpleado = roleRepository.findByNombre("ROLE_EMPLEADO").orElseThrow();
            return usuarioRepository.save(Usuario.builder()
                    .username("emp1")
                    .password(passwordEncoder.encode("secret123"))
                    .email("emp1@example.com")
                    .roles(Set.of(rolEmpleado))
                    .build());
        });
        empleadoId = emp.getId();
    }

    @Test
    @DisplayName("Un EMPLEADO puede crear una venta y queda atribuida a él")
    void empleadoCreaVenta() throws Exception {
        crearGrupoComoAdmin("EMP-G1", 5);

        mockMvc.perform(post("/api/v1/ventas").with(user("emp1").roles("EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaDTO(1, 300.0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vendedorUsername").value("emp1"));
    }

    @Test
    @DisplayName("Un EMPLEADO solo ve sus propias ventas en el listado")
    void empleadoSoloVeSusVentas() throws Exception {
        crearGrupoComoAdmin("EMP-G2", 10);
        // El admin hace una venta; el empleado hace otra.
        crearVenta(user("admin").roles("ADMIN"), ventaDTO(1, 300.0));
        crearVenta(user("emp1").roles("EMPLEADO"), ventaDTO(1, 300.0));

        MvcResult res = mockMvc.perform(get("/api/v1/ventas").with(user("emp1").roles("EMPLEADO")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(res.getResponse().getContentAsString());
        JsonNode content = body.get("content");
        assertTrue(content.size() >= 1, "El empleado debe ver al menos su venta");
        for (JsonNode venta : content) {
            assertEquals("emp1", venta.get("vendedorUsername").asText(),
                    "El empleado no debe ver ventas de otros");
        }
    }

    @Test
    @DisplayName("El ADMIN puede filtrar ventas por empleado (vendedorId)")
    void adminFiltraPorEmpleado() throws Exception {
        crearGrupoComoAdmin("EMP-G3", 10);
        crearVenta(user("admin").roles("ADMIN"), ventaDTO(1, 300.0));
        crearVenta(user("emp1").roles("EMPLEADO"), ventaDTO(1, 300.0));

        MvcResult res = mockMvc.perform(get("/api/v1/ventas")
                        .param("vendedorId", String.valueOf(empleadoId))
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(res.getResponse().getContentAsString()).get("content");
        for (JsonNode venta : content) {
            assertEquals("emp1", venta.get("vendedorUsername").asText());
        }
    }

    @Test
    @DisplayName("Un EMPLEADO no puede leer reportes financieros (403)")
    void empleadoNoVeReportesFinancieros() throws Exception {
        mockMvc.perform(get("/api/v1/ventas/ingresos-totales").with(user("emp1").roles("EMPLEADO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El ADMIN accede al ranking de ventas por empleado")
    void adminVeRankingPorEmpleado() throws Exception {
        mockMvc.perform(get("/api/v1/ventas/por-empleado").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un EMPLEADO no puede acceder al ranking por empleado (403)")
    void empleadoNoVeRanking() throws Exception {
        mockMvc.perform(get("/api/v1/ventas/por-empleado").with(user("emp1").roles("EMPLEADO")))
                .andExpect(status().isForbidden());
    }

    // ----- helpers -----

    private void crearVenta(org.springframework.test.web.servlet.request.RequestPostProcessor quien,
                            SolicitudCompraRequestDTO dto) throws Exception {
        mockMvc.perform(post("/api/v1/ventas").with(quien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    private void crearGrupoComoAdmin(String codigo, int stock) throws Exception {
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

        mockMvc.perform(post("/api/v1/grupos-electrogenos").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grupo)))
                .andExpect(status().isCreated());
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
}

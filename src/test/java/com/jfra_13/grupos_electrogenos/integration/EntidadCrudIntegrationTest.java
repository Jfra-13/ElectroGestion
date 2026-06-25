package com.jfra_13.grupos_electrogenos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfra_13.grupos_electrogenos.model.dto.EntidadRequestDTO;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de aceptación del CRUD de clientes (entidades) y no-regresión de
 * rankings/reportes. Cubre: crear (201), buscar (200), duplicado (409),
 * permisos (EMPLEADO crea/lista, sin token 401) y que una venta contra un
 * cliente nuevo aparezca en ranking-clientes y por-empleado.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class EntidadCrudIntegrationTest {

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

    @BeforeEach
    void seedEmpleado() {
        usuarioRepository.findByUsername("emp1").orElseGet(() -> {
            Role rolEmpleado = roleRepository.findByNombre("ROLE_EMPLEADO").orElseThrow();
            return usuarioRepository.save(Usuario.builder()
                    .username("emp1")
                    .password(passwordEncoder.encode("secret123"))
                    .email("emp1@example.com")
                    .roles(Set.of(rolEmpleado))
                    .build());
        });
    }

    @Test
    @DisplayName("POST /clientes crea un cliente y devuelve 201 con id")
    void crearClienteDevuelve201() throws Exception {
        mockMvc.perform(post("/api/v1/clientes").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonNombre("Constructora del Sur S.A.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Constructora del Sur S.A."));
    }

    @Test
    @DisplayName("GET /clientes?nombre= filtra por coincidencia parcial (autocomplete)")
    void buscarPorNombreFiltra() throws Exception {
        crearCliente(user("admin").roles("ADMIN"), "Acme Generadores");
        crearCliente(user("admin").roles("ADMIN"), "Beta Energía");

        mockMvc.perform(get("/api/v1/clientes").param("nombre", "acme")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Acme Generadores"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Crear cliente duplicado (case-insensitive) devuelve 409, no 500")
    void duplicadoDevuelve409() throws Exception {
        crearCliente(user("admin").roles("ADMIN"), "Duplicado SRL");

        mockMvc.perform(post("/api/v1/clientes").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonNombre("duplicado srl")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Nombre vacío devuelve 400")
    void nombreVacioDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/clientes").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonNombre(" ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un EMPLEADO puede crear y listar clientes")
    void empleadoPuedeCrearYListar() throws Exception {
        crearCliente(user("emp1").roles("EMPLEADO"), "Cliente del Empleado");

        mockMvc.perform(get("/api/v1/clientes").with(user("emp1").roles("EMPLEADO")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sin token, /clientes es rechazado (4xx)")
    void sinTokenRechazado() throws Exception {
        // En el slice de MockMvc no hay AuthenticationEntryPoint del filtro JWT, así que
        // la falta de credenciales se traduce a 403; en runtime con JWT real es 401.
        mockMvc.perform(get("/api/v1/clientes"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("No-regresión: venta contra cliente nuevo aparece en ranking-clientes y por-empleado")
    void ventaConClienteNuevoSeRefleja() throws Exception {
        crearGrupoComoAdmin("CLI-G1", 5);
        Long clienteId = crearCliente(user("emp1").roles("EMPLEADO"), "Cliente Ranking SA");

        mockMvc.perform(post("/api/v1/ventas").with(user("emp1").roles("EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ventaDTO(clienteId, 1, 300.0))))
                .andExpect(status().isCreated());

        MvcResult rankingClientes = mockMvc.perform(get("/api/v1/ventas/ranking-clientes")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(rankingClientes.getResponse().getContentAsString().contains("Cliente Ranking SA"),
                "El cliente nuevo debe aparecer en ranking-clientes");

        MvcResult porEmpleado = mockMvc.perform(get("/api/v1/ventas/por-empleado")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(porEmpleado.getResponse().getContentAsString().contains("emp1"),
                "El empleado vendedor debe aparecer en por-empleado");
    }

    // ----- helpers -----

    private String jsonNombre(String nombre) throws Exception {
        return objectMapper.writeValueAsString(new EntidadRequestDTO(nombre));
    }

    private Long crearCliente(org.springframework.test.web.servlet.request.RequestPostProcessor quien,
                              String nombre) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/clientes").with(quien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonNombre(nombre)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(res.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private void crearGrupoComoAdmin(String codigo, int stock) throws Exception {
        GrupoElectrogenoRequestDTO grupo = GrupoElectrogenoRequestDTO.builder()
                .codigo(codigo)
                .vidaUtil(10)
                .tipoCombustible(TipoCombustible.GASOIL)
                .tipoArranque(TipoArranque.MANUAL)
                .pMin(100.0)
                .pMax(999999.0)
                .esMovil(false)
                .stock(stock)
                .build();

        mockMvc.perform(post("/api/v1/grupos-electrogenos").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grupo)))
                .andExpect(status().isCreated());
    }

    private SolicitudCompraRequestDTO ventaDTO(Long entidadId, int cantidad, double potencia) {
        SolicitudCompraRequestDTO dto = new SolicitudCompraRequestDTO();
        dto.setEntidadId(entidadId);
        dto.setNombreSolicitante("Comprador Test");
        dto.setCantidad(cantidad);
        dto.setPotenciaRequerida(potencia);
        dto.setTipoCombustible(TipoCombustible.GASOIL);
        dto.setTipoPago(TipoPago.EFECTIVO);
        dto.setVidaUtilSolicitada(5);
        return dto;
    }
}

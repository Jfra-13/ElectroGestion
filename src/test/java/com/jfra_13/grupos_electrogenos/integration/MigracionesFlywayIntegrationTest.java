package com.jfra_13.grupos_electrogenos.integration;

import com.jfra_13.grupos_electrogenos.model.entity.Role;
import com.jfra_13.grupos_electrogenos.model.entity.Usuario;
import com.jfra_13.grupos_electrogenos.repository.RoleRepository;
import com.jfra_13.grupos_electrogenos.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de aceptación de la Fase 2 contra PostgreSQL real (Testcontainers).
 *
 * Cubre I6 (Flyway crea el esquema sobre una base vacía y JPA {@code validate}
 * pasa) e I7 (roles sembrados + primer admin con ROLE_ADMIN). Requiere Docker.
 */
@SpringBootTest
@ActiveProfiles("prod")
@Testcontainers
class MigracionesFlywayIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void overrideSecrets(DynamicPropertyRegistry registry) {
        // Secretos del perfil prod que normalmente vienen del entorno.
        registry.add("jwt.secret", () -> "secreto_de_test_suficientemente_largo_para_HS256_0123456789");
        registry.add("app.bootstrap.admin.username", () -> "admin");
        registry.add("app.bootstrap.admin.password", () -> "admin-pass-123");
        registry.add("app.bootstrap.admin.email", () -> "admin@test.local");
    }

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("I6: Flyway crea el esquema en base vacía y JPA validate pasa")
    void flywayCreaEsquemaYValidatePasa() {
        // Si el contexto levantó, Flyway aplicó V1/V2 y ddl-auto=validate no falló.
        assertTrue(roleRepository.findByNombre("ROLE_USER").isPresent(),
                "ROLE_USER debe estar sembrado por V2");
        assertTrue(roleRepository.findByNombre("ROLE_ADMIN").isPresent(),
                "ROLE_ADMIN debe estar sembrado por V2");
    }

    @Test
    @DisplayName("I7: existe un primer admin con ROLE_ADMIN")
    void primerAdminSembrado() {
        Usuario admin = usuarioRepository.findByUsername("admin")
                .orElseThrow(() -> new AssertionError("El admin inicial debe existir"));
        Set<String> roles = admin.getRoles().stream()
                .map(Role::getNombre)
                .collect(Collectors.toSet());
        assertTrue(roles.contains("ROLE_ADMIN"), "El admin inicial debe tener ROLE_ADMIN");
    }
}

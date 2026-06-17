package com.jfra_13.grupos_electrogenos.security;

import com.jfra_13.grupos_electrogenos.GruposElectrogenosApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de aceptación de I3 (Fase 1): el perfil de producción debe fallar al
 * arrancar si falta un secreto, en vez de bootear con un default conocido.
 *
 * Se sustituye la base de datos por H2 para aislar la causa del fallo: lo único
 * que falta es el secreto JWT de producción ({@code JWT_SECRET_PROD}), por lo que
 * el arranque debe romper por el placeholder sin resolver, probando el fail-fast.
 */
class ProdSecretosFailFastTest {

    @Test
    @DisplayName("I3: arrancar el perfil prod sin JWT_SECRET_PROD falla (no boot con default)")
    void prodSinSecretoJwtNoArranca() {
        SpringApplicationBuilder app = new SpringApplicationBuilder(GruposElectrogenosApplication.class)
                .profiles("prod")
                .web(WebApplicationType.NONE)
                .properties(
                        // DB en H2 para que la única causa de fallo sea el secreto faltante.
                        "spring.datasource.url=jdbc:h2:mem:failfast;DB_CLOSE_DELAY=-1",
                        "spring.datasource.driverClassName=org.h2.Driver",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=sa",
                        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                        "spring.jpa.hibernate.ddl-auto=none",
                        "spring.sql.init.mode=never"
                        // NO se define JWT_SECRET_PROD: el placeholder de jwt.secret quedará sin resolver.
                );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            try (ConfigurableApplicationContext ignored = app.run()) {
                // No debe llegar aquí: el contexto no debe levantar sin el secreto.
            }
        });

        String causa = raizMensaje(ex);
        assertTrue(causa.contains("JWT_SECRET_PROD") || causa.toLowerCase().contains("placeholder"),
                "El arranque debe fallar por el secreto JWT faltante, no por un default. Causa: " + causa);
    }

    private static String raizMensaje(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable cur = t;
        while (cur != null) {
            if (cur.getMessage() != null) {
                sb.append(cur.getMessage()).append(" | ");
            }
            cur = cur.getCause();
        }
        return sb.toString();
    }
}

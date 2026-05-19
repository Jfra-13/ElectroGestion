package com.jfra_13.grupos_electrogenos.integration;

import com.jfra_13.grupos_electrogenos.model.entity.Entidad;
import com.jfra_13.grupos_electrogenos.repository.EntidadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class EntidadSeedIntegrationTest {

    @Autowired
    private EntidadRepository entidadRepository;

    @Test
    void debeCargarEntidadSemillaConIdFijo() {
        assertTrue(entidadRepository.existsById(1L), "Debe existir la entidad semilla con ID 1");

        Entidad entidad = entidadRepository.findById(1L).orElseThrow();
        assertEquals("Empresa Demo", entidad.getNombre());
    }
}


package com.jfra_13.grupos_electrogenos.repository;

import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogenoMovil;
import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class GrupoElectrogenoRepositoryTest {

    @Autowired
    private GrupoElectrogenoRepository repository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @DisplayName("Debe guardar un Grupo Electrógeno Móvil correctamente y generar timestamps")
    void testGuardarGrupoMovil() {
        // Arrange
        GrupoElectrogenoMovil movil = new GrupoElectrogenoMovil();
        movil.setCodigo("MOV-001");
        movil.setVidaUtil(10);
        movil.setTipoCombustible(TipoCombustible.GASOIL);
        movil.setTipoArranque(TipoArranque.AUTOMATICO);
        movil.setPMin(100.0);
        movil.setPMax(150.0);
        movil.setInsonorizado(true);
        movil.setCapo(true);
        movil.setCantidadRuedas(4);
        movil.setMaterialEje(MaterialEje.ACERO);

        // Act
        GrupoElectrogenoMovil guardado = repository.save(movil);
        entityManager.flush();

        // Assert
        assertThat(guardado).isNotNull();
        assertThat(guardado.getId()).isGreaterThan(0);
        assertThat(guardado.getCreatedAt()).isNotNull();
        assertThat(guardado.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Debe fallar si pMin >= pMax")
    void testValidacionPotencia() {
        GrupoElectrogenoMovil movil = new GrupoElectrogenoMovil();
        movil.setCodigo("MOV-ERR");
        movil.setVidaUtil(5);
        movil.setTipoCombustible(TipoCombustible.GASOIL);
        movil.setTipoArranque(TipoArranque.MANUAL);
        movil.setPMin(500.0);
        movil.setPMax(100.0); // Error: pMin > pMax
        movil.setCantidadRuedas(2);
        movil.setMaterialEje(MaterialEje.ALEACION);

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            repository.save(movil);
            entityManager.flush();
        });
    }
}
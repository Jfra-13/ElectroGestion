package com.jfra_13.grupos_electrogenos.repository;

import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
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
    @DisplayName("Debe buscar correctamente por combustible y ordenar por potencia máxima")
    void testBuscarPorCombustible() {
        // Arrange
        GrupoElectrogeno g1 = new GrupoElectrogeno();
        g1.setCodigo("G1");
        g1.setVidaUtil(10);
        g1.setTipoCombustible(TipoCombustible.GASOIL);
        g1.setTipoArranque(TipoArranque.MANUAL);
        g1.setPMin(100.0);
        g1.setPMax(500.0);

        GrupoElectrogeno g2 = new GrupoElectrogeno();
        g2.setCodigo("G2");
        g2.setVidaUtil(10);
        g2.setTipoCombustible(TipoCombustible.GASOIL);
        g2.setTipoArranque(TipoArranque.MANUAL);
        g2.setPMin(100.0);
        g2.setPMax(1000.0);

        repository.save(g1);
        repository.save(g2);
        entityManager.flush();

        // Act
        java.util.List<GrupoElectrogeno> resultados = repository.findByTipoCombustibleOrderByPMaxDesc(TipoCombustible.GASOIL, org.springframework.data.domain.Pageable.unpaged()).getContent();

        // Assert
        assertThat(resultados).hasSize(2);
        assertThat(resultados.get(0).getCodigo()).isEqualTo("G2");
        assertThat(resultados.get(1).getCodigo()).isEqualTo("G1");
    }

    @Test
    @DisplayName("Debe fallar si el código está duplicado")
    void testCodigoDuplicado() {
        GrupoElectrogeno g1 = new GrupoElectrogeno();
        g1.setCodigo("DUP");
        g1.setVidaUtil(10);
        g1.setTipoCombustible(TipoCombustible.GASOIL);
        g1.setTipoArranque(TipoArranque.MANUAL);
        g1.setPMin(10.0);
        g1.setPMax(20.0);
        repository.save(g1);

        GrupoElectrogeno g2 = new GrupoElectrogeno();
        g2.setCodigo("DUP");
        g2.setVidaUtil(5);
        g2.setTipoCombustible(TipoCombustible.NAFTA);
        g2.setTipoArranque(TipoArranque.AUTOMATICO);
        g2.setPMin(5.0);
        g2.setPMax(15.0);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            repository.save(g2);
            entityManager.flush();
        });
    }
}
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

@DataJpaTest
class GrupoElectrogenoRepositoryTest {

    @Autowired
    private GrupoElectrogenoRepository repository;

    @Test
    @DisplayName("Debe guardar un Grupo Electrógeno Móvil correctamente")
    void testGuardarGrupoMovil() {
        // Arrange: Preparamos los datos
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

        // Act: Ejecutamos la acción en la base de datos
        GrupoElectrogenoMovil guardado = repository.save(movil);

        // Assert: Verificamos que se guardó y generó un ID
        assertThat(guardado).isNotNull();
        assertThat(guardado.getId()).isGreaterThan(0);
        assertThat(guardado.getCodigo()).isEqualTo("MOV-001");
    }
}
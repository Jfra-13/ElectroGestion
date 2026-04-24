package com.jfra_13.grupos_electrogenos.service;

import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogenoMovil;
import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.repository.GrupoElectrogenoRepository;
import com.jfra_13.grupos_electrogenos.service.impl.GrupoElectrogenoServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GrupoElectrogenoServiceTest {

    @Mock
    private GrupoElectrogenoRepository repository; // Simulamos la base de datos

    @InjectMocks
    private GrupoElectrogenoServiceImpl service; // Inyectamos el mock en nuestro servicio

    @Test
    @DisplayName("Debe calcular el precio correctamente para un Grupo Fijo (Equipado)")
    void testCalcularPrecioFijo() {
        // Arrange
        GrupoElectrogeno fijo = new GrupoElectrogeno();
        fijo.setVidaUtil(10);
        fijo.setPMin(100.0);
        fijo.setPMax(200.0); // Potencia media = 150. Base = 10 * 150 = 1500
        fijo.setInsonorizado(true);
        fijo.setCapo(true); // + 10
        fijo.setTipoArranque(TipoArranque.AUTOMATICO); // + 15
        // Valor agregado por ser fijo = + 200
        // Total esperado = 1500 + 10 + 15 + 200 = 1725

        // Act
        Double precio = service.calcularPrecioVenta(fijo);

        // Assert
        assertThat(precio).isEqualTo(1725.0);
    }

    @Test
    @DisplayName("Debe calcular el precio correctamente para un Grupo Móvil Básico con Acero")
    void testCalcularPrecioMovilAcero() {
        // Arrange
        GrupoElectrogenoMovil movil = new GrupoElectrogenoMovil();
        movil.setVidaUtil(5);
        movil.setPMin(50.0);
        movil.setPMax(150.0); // Potencia media = 100. Base = 5 * 100 = 500
        movil.setInsonorizado(false);
        movil.setCapo(false); // + 0
        movil.setTipoArranque(TipoArranque.MANUAL); // + 0
        movil.setCantidadRuedas(4); // 4 * 5 = 20
        movil.setMaterialEje(MaterialEje.ACERO); // + 20
        // Total esperado = 500 + 20 + 20 = 540

        // Act
        Double precio = service.calcularPrecioVenta(movil);

        // Assert
        assertThat(precio).isEqualTo(540.0);
    }
}
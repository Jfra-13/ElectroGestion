package com.jfra_13.grupos_electrogenos.repository;

import com.jfra_13.grupos_electrogenos.model.entity.Entidad;
import com.jfra_13.grupos_electrogenos.model.entity.SolicitudCompra;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SolicitudCompraRepositoryTest {

    @Autowired
    private SolicitudCompraRepository solicitudRepository;

    @Autowired
    private EntidadRepository entidadRepository;

    @Test
    @DisplayName("Debe guardar una Solicitud de Compra vinculada a una Entidad")
    void testGuardarSolicitud() {
        // Arrange: Primero creamos y guardamos la Entidad
        Entidad empresa = new Entidad();
        empresa.setNombre("Constructora ABC");
        Entidad empresaGuardada = entidadRepository.save(empresa);

        // Preparamos la solicitud
        SolicitudCompra solicitud = new SolicitudCompra();
        solicitud.setIdentificador("SOL-100");
        solicitud.setNombreSolicitante("Carlos Pérez");
        solicitud.setTipoPago(TipoPago.CHEQUE);
        solicitud.setCantidad(2);
        solicitud.setPotenciaRequerida(120.0);
        solicitud.setTipoCombustible(TipoCombustible.GAS_NATURAL);
        solicitud.setVidaUtilSolicitada(5);
        solicitud.setEntidad(empresaGuardada); // Vinculamos la llave foránea

        // Act
        SolicitudCompra solicitudGuardada = solicitudRepository.save(solicitud);

        // Assert
        assertThat(solicitudGuardada).isNotNull();
        assertThat(solicitudGuardada.getId()).isGreaterThan(0);
        assertThat(solicitudGuardada.getEntidad().getNombre()).isEqualTo("Constructora ABC");
    }
}
package com.jfra_13.grupos_electrogenos.repository;

import com.jfra_13.grupos_electrogenos.model.entity.Entidad;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
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
    @Autowired
    private GrupoElectrogenoRepository grupoRepository;

    @Test
    @DisplayName("Debe guardar una Solicitud de Compra vinculada a una Entidad y un Grupo")
    void testGuardarSolicitud() {
        Entidad empresa = new Entidad();
        empresa.setNombre("Constructora ABC");
        Entidad empresaGuardada = entidadRepository.save(empresa);
        GrupoElectrogeno grupo = new GrupoElectrogeno();
        grupo.setCodigo("TEST-001");
        grupo.setTipoCombustible(TipoCombustible.GAS_NATURAL);
        grupo.setTipoArranque(com.jfra_13.grupos_electrogenos.model.enums.TipoArranque.MANUAL);
        grupo.setVidaUtil(10);
        grupo.setPMin(50.0);
        grupo.setPMax(200.0);
        GrupoElectrogeno grupoGuardado = grupoRepository.save(grupo);
        SolicitudCompra solicitud = new SolicitudCompra();
        solicitud.setIdentificador("SOL-100");
        solicitud.setNombreSolicitante("Carlos P�rez");
        solicitud.setTipoPago(TipoPago.CHEQUE);
        solicitud.setCantidad(2);
        solicitud.setPotenciaRequerida(120.0);
        solicitud.setTipoCombustible(TipoCombustible.GAS_NATURAL);
        solicitud.setVidaUtilSolicitada(5);
        solicitud.setEntidad(empresaGuardada);
        solicitud.setGrupoElectrogeno(grupoGuardado);
        SolicitudCompra solicitudGuardada = solicitudRepository.save(solicitud);
        assertThat(solicitudGuardada).isNotNull();
        assertThat(solicitudGuardada.getId()).isGreaterThan(0);
        assertThat(solicitudGuardada.getEntidad().getNombre()).isEqualTo("Constructora ABC");
        assertThat(solicitudGuardada.getGrupoElectrogeno().getCodigo()).isEqualTo("TEST-001");
    }
}

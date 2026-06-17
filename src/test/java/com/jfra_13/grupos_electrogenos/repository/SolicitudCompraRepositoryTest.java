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
    @DisplayName("Debe fallar si el identificador está duplicado")
    void testIdentificadorDuplicado() {
        Entidad e = new Entidad();
        e.setNombre("E1");
        entidadRepository.save(e);

        GrupoElectrogeno g = new GrupoElectrogeno();
        g.setCodigo("G1");
        g.setTipoCombustible(TipoCombustible.GASOIL);
        g.setTipoArranque(com.jfra_13.grupos_electrogenos.model.enums.TipoArranque.MANUAL);
        g.setVidaUtil(10);
        g.setPMin(10.0);
        g.setPMax(20.0);
        grupoRepository.save(g);

        SolicitudCompra s1 = new SolicitudCompra();
        s1.setIdentificador("DUP");
        s1.setNombreSolicitante("S1");
        s1.setTipoPago(TipoPago.EFECTIVO);
        s1.setCantidad(1);
        s1.setPotenciaRequerida(15.0);
        s1.setTipoCombustible(TipoCombustible.GASOIL);
        s1.setVidaUtilSolicitada(5);
        s1.setEntidad(e);
        s1.setGrupoElectrogeno(g);
        s1.setPrecioUnitario(150.0); // precio/total congelados (columnas NOT NULL)
        s1.setTotal(150.0);
        solicitudRepository.save(s1);

        SolicitudCompra s2 = new SolicitudCompra();
        s2.setIdentificador("DUP");
        s2.setNombreSolicitante("S2");
        s2.setTipoPago(TipoPago.EFECTIVO);
        s2.setCantidad(1);
        s2.setPotenciaRequerida(15.0);
        s2.setTipoCombustible(TipoCombustible.GASOIL);
        s2.setVidaUtilSolicitada(5);
        s2.setEntidad(e);
        s2.setGrupoElectrogeno(g);
        s2.setPrecioUnitario(150.0);
        s2.setTotal(150.0);

        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            solicitudRepository.save(s2);
            solicitudRepository.flush();
        });
    }
}

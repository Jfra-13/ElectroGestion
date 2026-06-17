package com.jfra_13.grupos_electrogenos.mapper;

import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraResponseDTO;
import com.jfra_13.grupos_electrogenos.model.entity.Entidad;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.SolicitudCompra;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitudCompraMapperTest {

    private final SolicitudCompraMapper mapper = Mappers.getMapper(SolicitudCompraMapper.class);

    @Test
    void debeMapearRequestAEntidadConRelaciones() {
        SolicitudCompraRequestDTO dto = new SolicitudCompraRequestDTO();
        dto.setNombreSolicitante("Juan");
        dto.setTipoPago(TipoPago.EFECTIVO);
        dto.setCantidad(2);
        dto.setPotenciaRequerida(300.0);
        dto.setTipoCombustible(TipoCombustible.GASOIL);
        dto.setVidaUtilSolicitada(5);

        Entidad entidad = new Entidad();
        entidad.setId(1L);
        entidad.setNombre("ACME");

        GrupoElectrogeno grupo = new GrupoElectrogeno();
        grupo.setId(2L);
        grupo.setCodigo("G-1");

        SolicitudCompra entity = mapper.toEntity(dto, entidad, grupo, "ABC123");

        assertThat(entity.getIdentificador()).isEqualTo("ABC123");
        assertThat(entity.getNombreSolicitante()).isEqualTo("Juan");
        assertThat(entity.getEntidad()).isSameAs(entidad);
        assertThat(entity.getGrupoElectrogeno()).isSameAs(grupo);
    }

    @Test
    void debeMapearEntidadADtoConDatosRelacionadosYPrecio() {
        Entidad entidad = new Entidad();
        entidad.setId(1L);
        entidad.setNombre("ACME");

        GrupoElectrogeno grupo = new GrupoElectrogeno();
        grupo.setId(2L);
        grupo.setCodigo("G-1");

        SolicitudCompra entity = new SolicitudCompra();
        entity.setId(9L);
        entity.setIdentificador("ABC123");
        entity.setNombreSolicitante("Juan");
        entity.setTipoPago(TipoPago.CHEQUE);
        entity.setCantidad(3);
        entity.setPotenciaRequerida(400.0);
        entity.setTipoCombustible(TipoCombustible.NAFTA);
        entity.setVidaUtilSolicitada(6);
        entity.setEntidad(entidad);
        entity.setGrupoElectrogeno(grupo);
        // Precio/total congelados en la entidad: el response los lee tal cual.
        entity.setPrecioUnitario(777.0);
        entity.setTotal(2331.0);

        SolicitudCompraResponseDTO dto = mapper.toResponse(entity);

        assertThat(dto.getId()).isEqualTo(9L);
        assertThat(dto.getEntidadId()).isEqualTo(1L);
        assertThat(dto.getEntidadNombre()).isEqualTo("ACME");
        assertThat(dto.getGrupoId()).isEqualTo(2L);
        assertThat(dto.getGrupoCodigo()).isEqualTo("G-1");
        assertThat(dto.getPrecioVentaUnitario()).isEqualTo(777.0);
        assertThat(dto.getTotal()).isEqualTo(2331.0);
    }
}


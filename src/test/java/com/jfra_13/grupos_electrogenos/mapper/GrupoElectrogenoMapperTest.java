package com.jfra_13.grupos_electrogenos.mapper;

import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoResponseDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoMovilResumenDTO;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogenoMovil;
import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class GrupoElectrogenoMapperTest {

    private final GrupoElectrogenoMapper mapper = Mappers.getMapper(GrupoElectrogenoMapper.class);

    @Test
    void debeMapearRequestAMovilCuandoEsMovil() {
        GrupoElectrogenoRequestDTO dto = GrupoElectrogenoRequestDTO.builder()
                .codigo("MOV-1")
                .vidaUtil(8)
                .tipoCombustible(TipoCombustible.GASOIL)
                .tipoArranque(TipoArranque.AUTOMATICO)
                .pMin(100.0)
                .pMax(200.0)
                .insonorizado(true)
                .capo(false)
                .cantidadRuedas(4)
                .materialEje(MaterialEje.ACERO)
                .esMovil(true)
                .build();

        GrupoElectrogeno entity = mapper.toEntity(dto);

        assertThat(entity).isInstanceOf(GrupoElectrogenoMovil.class);
        GrupoElectrogenoMovil movil = (GrupoElectrogenoMovil) entity;
        assertThat(movil.getCodigo()).isEqualTo("MOV-1");
        assertThat(movil.getCantidadRuedas()).isEqualTo(4);
        assertThat(movil.getMaterialEje()).isEqualTo(MaterialEje.ACERO);
    }

    @Test
    void debeMapearEntidadMovilADtoConCamposDerivados() {
        GrupoElectrogenoMovil entity = new GrupoElectrogenoMovil();
        entity.setId(10L);
        entity.setCodigo("MOV-1");
        entity.setVidaUtil(8);
        entity.setTipoCombustible(TipoCombustible.GASOIL);
        entity.setTipoArranque(TipoArranque.AUTOMATICO);
        entity.setPMin(100.0);
        entity.setPMax(200.0);
        entity.setInsonorizado(true);
        entity.setCapo(false);
        entity.setCantidadRuedas(4);
        entity.setMaterialEje(MaterialEje.ACERO);

        GrupoElectrogenoResponseDTO dto = mapper.toResponse(entity, 1234.5);

        assertThat(dto.getCodigo()).isEqualTo("MOV-1");
        assertThat(dto.getPotenciaMedia()).isEqualTo(150.0);
        assertThat(dto.getPrecioVentaCalculado()).isEqualTo(1234.5);
        assertThat(dto.getTipoGrupo()).isEqualTo("Móvil");
        assertThat(dto.getCantidadRuedas()).isEqualTo(4);
        assertThat(dto.getMaterialEje()).isEqualTo(MaterialEje.ACERO);
    }

    @Test
    void debeMapearEntidadFijaADtoConTipoGrupoFijo() {
        GrupoElectrogeno entity = new GrupoElectrogeno();
        entity.setId(11L);
        entity.setCodigo("FIX-1");
        entity.setVidaUtil(10);
        entity.setTipoCombustible(TipoCombustible.NAFTA);
        entity.setTipoArranque(TipoArranque.MANUAL);
        entity.setPMin(50.0);
        entity.setPMax(150.0);
        entity.setInsonorizado(false);
        entity.setCapo(true);

        GrupoElectrogenoResponseDTO dto = mapper.toResponse(entity, 999.0);

        assertThat(dto.getTipoGrupo()).isEqualTo("Fijo");
        assertThat(dto.getPotenciaMedia()).isEqualTo(100.0);
        assertThat(dto.getPrecioVentaCalculado()).isEqualTo(999.0);
    }

    @Test
    void debeMapearResumenMovil() {
        GrupoElectrogenoMovil entity = new GrupoElectrogenoMovil();
        entity.setCodigo("MOV-2");
        entity.setVidaUtil(6);

        GrupoMovilResumenDTO dto = mapper.toResumen(entity);

        assertThat(dto.getCodigo()).isEqualTo("MOV-2");
        assertThat(dto.getVidaUtil()).isEqualTo(6);
    }
}


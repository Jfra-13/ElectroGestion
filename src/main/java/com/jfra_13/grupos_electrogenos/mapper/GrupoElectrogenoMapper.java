package com.jfra_13.grupos_electrogenos.mapper;

import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoResponseDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoMovilResumenDTO;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogenoMovil;
import org.mapstruct.AfterMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public interface GrupoElectrogenoMapper {

    GrupoElectrogeno toEntity(GrupoElectrogenoRequestDTO dto);

    void updateEntity(GrupoElectrogenoRequestDTO dto, @MappingTarget GrupoElectrogeno entity);

    @Mapping(target = "potenciaMedia", expression = "java(calcularPotenciaMedia(entity.getPMin(), entity.getPMax()))")
    @Mapping(target = "precioVentaCalculado", source = "precioVentaCalculado")
    @Mapping(target = "tipoGrupo", ignore = true)
    GrupoElectrogenoResponseDTO toResponseBase(GrupoElectrogeno entity, Double precioVentaCalculado);

    @InheritConfiguration(name = "toResponseBase")
    GrupoElectrogenoResponseDTO toResponse(GrupoElectrogenoMovil entity, Double precioVentaCalculado);

    @Mapping(target = "codigo", source = "codigo")
    @Mapping(target = "vidaUtil", source = "vidaUtil")
    GrupoMovilResumenDTO toResumen(GrupoElectrogenoMovil entity);

    default GrupoElectrogenoResponseDTO toResponse(GrupoElectrogeno entity, Double precioVentaCalculado) {
        if (entity instanceof GrupoElectrogenoMovil movil) {
            return toResponse(movil, precioVentaCalculado);
        }
        return toResponseBase(entity, precioVentaCalculado);
    }

    @ObjectFactory
    default GrupoElectrogeno createEntity(GrupoElectrogenoRequestDTO dto) {
        return Boolean.TRUE.equals(dto.getEsMovil()) ? new GrupoElectrogenoMovil() : new GrupoElectrogeno();
    }

    @AfterMapping
    default void afterToEntity(GrupoElectrogenoRequestDTO dto, @MappingTarget GrupoElectrogeno entity) {
        if (entity instanceof GrupoElectrogenoMovil movil && Boolean.TRUE.equals(dto.getEsMovil())) {
            movil.setCantidadRuedas(dto.getCantidadRuedas());
            movil.setMaterialEje(dto.getMaterialEje());
        }
    }

    @AfterMapping
    default void afterToResponse(GrupoElectrogeno entity, @MappingTarget GrupoElectrogenoResponseDTO dto) {
        if (entity instanceof GrupoElectrogenoMovil movil) {
            dto.setTipoGrupo("Móvil");
            dto.setCantidadRuedas(movil.getCantidadRuedas());
            dto.setMaterialEje(movil.getMaterialEje());
        } else {
            dto.setTipoGrupo("Fijo");
        }
    }

    default Double calcularPotenciaMedia(Double pMin, Double pMax) {
        return (pMin + pMax) / 2.0;
    }
}


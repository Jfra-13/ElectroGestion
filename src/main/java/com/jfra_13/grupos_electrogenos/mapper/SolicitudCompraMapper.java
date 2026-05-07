package com.jfra_13.grupos_electrogenos.mapper;

import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraResponseDTO;
import com.jfra_13.grupos_electrogenos.model.entity.Entidad;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.SolicitudCompra;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SolicitudCompraMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "identificador", source = "identificador")
    @Mapping(target = "nombreSolicitante", source = "dto.nombreSolicitante")
    @Mapping(target = "tipoPago", source = "dto.tipoPago")
    @Mapping(target = "cantidad", source = "dto.cantidad")
    @Mapping(target = "potenciaRequerida", source = "dto.potenciaRequerida")
    @Mapping(target = "tipoCombustible", source = "dto.tipoCombustible")
    @Mapping(target = "vidaUtilSolicitada", source = "dto.vidaUtilSolicitada")
    @Mapping(target = "entidad", source = "entidad")
    @Mapping(target = "grupoElectrogeno", source = "grupoElectrogeno")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SolicitudCompra toEntity(SolicitudCompraRequestDTO dto, Entidad entidad, GrupoElectrogeno grupoElectrogeno, String identificador);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "identificador", ignore = true)
    @Mapping(target = "nombreSolicitante", source = "dto.nombreSolicitante")
    @Mapping(target = "tipoPago", source = "dto.tipoPago")
    @Mapping(target = "cantidad", source = "dto.cantidad")
    @Mapping(target = "potenciaRequerida", source = "dto.potenciaRequerida")
    @Mapping(target = "tipoCombustible", source = "dto.tipoCombustible")
    @Mapping(target = "vidaUtilSolicitada", source = "dto.vidaUtilSolicitada")
    @Mapping(target = "entidad", source = "entidad")
    @Mapping(target = "grupoElectrogeno", source = "grupoElectrogeno")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(SolicitudCompraRequestDTO dto, Entidad entidad, GrupoElectrogeno grupoElectrogeno, @MappingTarget SolicitudCompra entity);

    @Mapping(target = "entidadId", source = "entity.entidad.id")
    @Mapping(target = "entidadNombre", source = "entity.entidad.nombre")
    @Mapping(target = "grupoId", source = "entity.grupoElectrogeno.id")
    @Mapping(target = "grupoCodigo", source = "entity.grupoElectrogeno.codigo")
    @Mapping(target = "precioVentaUnitario", source = "precioVentaUnitario")
    SolicitudCompraResponseDTO toResponse(SolicitudCompra entity, Double precioVentaUnitario);
}


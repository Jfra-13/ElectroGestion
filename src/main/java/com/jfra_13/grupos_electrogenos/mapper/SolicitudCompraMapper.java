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
    @Mapping(target = "precioUnitario", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "vendedor", ignore = true)
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
    @Mapping(target = "precioUnitario", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "vendedor", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(SolicitudCompraRequestDTO dto, Entidad entidad, GrupoElectrogeno grupoElectrogeno, @MappingTarget SolicitudCompra entity);

    // El response lee el precio/total CONGELADOS en la entidad; no recalcula desde el grupo.
    @Mapping(target = "entidadId", source = "entidad.id")
    @Mapping(target = "entidadNombre", source = "entidad.nombre")
    @Mapping(target = "grupoId", source = "grupoElectrogeno.id")
    @Mapping(target = "grupoCodigo", source = "grupoElectrogeno.codigo")
    @Mapping(target = "precioVentaUnitario", source = "precioUnitario")
    @Mapping(target = "total", source = "total")
    @Mapping(target = "vendedorId", source = "vendedor.id")
    @Mapping(target = "vendedorUsername", source = "vendedor.username")
    SolicitudCompraResponseDTO toResponse(SolicitudCompra entity);
}


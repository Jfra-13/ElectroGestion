package com.jfra_13.grupos_electrogenos.service.impl;

import com.jfra_13.grupos_electrogenos.exception.ResourceNotFoundException;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoResponseDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoMovilResumenDTO;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogenoMovil;
import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.repository.GrupoElectrogenoRepository;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GrupoElectrogenoServiceImpl implements GrupoElectrogenoService {

    private final GrupoElectrogenoRepository repository;

    public GrupoElectrogenoServiceImpl(GrupoElectrogenoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public GrupoElectrogenoResponseDTO guardarGrupo(GrupoElectrogenoRequestDTO dto) {
        GrupoElectrogeno entidad = mapToEntity(dto);
        GrupoElectrogeno guardado = repository.save(entidad);
        return mapToResponseDTO(guardado);
    }

    @Override
    public GrupoElectrogenoResponseDTO obtenerPorId(Long id) {
        GrupoElectrogeno entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo Electrógeno no encontrado con ID: " + id));
        return mapToResponseDTO(entidad);
    }

    @Override
    @Transactional
    public GrupoElectrogenoResponseDTO actualizarGrupo(Long id, GrupoElectrogenoRequestDTO dto) {
        GrupoElectrogeno existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo Electrógeno no encontrado con ID: " + id));

        existing.setCodigo(dto.getCodigo());
        existing.setVidaUtil(dto.getVidaUtil());
        existing.setTipoCombustible(dto.getTipoCombustible());
        existing.setTipoArranque(dto.getTipoArranque());
        existing.setPMin(dto.getPMin());
        existing.setPMax(dto.getPMax());
        existing.setInsonorizado(dto.getInsonorizado());
        existing.setCapo(dto.getCapo());

        if (existing instanceof GrupoElectrogenoMovil movilExistente && Boolean.TRUE.equals(dto.getEsMovil())) {
            movilExistente.setCantidadRuedas(dto.getCantidadRuedas());
            movilExistente.setMaterialEje(dto.getMaterialEje());
        }

        GrupoElectrogeno actualizado = repository.save(existing);
        return mapToResponseDTO(actualizado);
    }

    @Override
    @Transactional
    public void eliminarGrupo(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Grupo Electrógeno no encontrado con ID: " + id);
        }
        repository.deleteById(id);
    }

    @Override
    public Double calcularPrecioVenta(GrupoElectrogeno grupo) {
        if (grupo == null || grupo.getVidaUtil() == null || grupo.getPMin() == null || grupo.getPMax() == null) {
            throw new IllegalArgumentException("Faltan datos básicos para calcular el precio.");
        }

        double potenciaMedia = (grupo.getPMin() + grupo.getPMax()) / 2.0;
        double precio = grupo.getVidaUtil() * potenciaMedia;

        if (Boolean.TRUE.equals(grupo.getInsonorizado()) && Boolean.TRUE.equals(grupo.getCapo())) {
            precio += 10.0;
        }

        if (TipoArranque.AUTOMATICO.equals(grupo.getTipoArranque())) {
            precio += 15.0;
        }

        if (grupo instanceof GrupoElectrogenoMovil movil) {
            double valorAgregado = (movil.getCantidadRuedas() != null ? movil.getCantidadRuedas() : 0) * 5.0;
            if (MaterialEje.ACERO.equals(movil.getMaterialEje())) {
                valorAgregado += 20.0;
            } else if (MaterialEje.ALEACION.equals(movil.getMaterialEje())) {
                valorAgregado += 13.0;
            }
            precio += valorAgregado;
        } else {
            precio += 200.0;
        }

        return precio;
    }

    @Override
    public List<GrupoElectrogenoResponseDTO> buscarPorCombustible(TipoCombustible combustible) {
        return repository.findByTipoCombustibleOrderByPMaxDesc(combustible).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<GrupoMovilResumenDTO> buscarMovilesPorEje(MaterialEje material) {
        List<GrupoElectrogenoMovil> moviles = repository.buscarMovilesAutomaticosPorEje(material);
        return moviles.stream()
                .map(m -> GrupoMovilResumenDTO.builder()
                        .codigo(m.getCodigo())
                        .vidaUtil(m.getVidaUtil())
                        .build())
                .collect(Collectors.toList());
    }

    private GrupoElectrogeno mapToEntity(GrupoElectrogenoRequestDTO dto) {
        GrupoElectrogeno entidad;
        if (Boolean.TRUE.equals(dto.getEsMovil())) {
            GrupoElectrogenoMovil movil = new GrupoElectrogenoMovil();
            movil.setCantidadRuedas(dto.getCantidadRuedas());
            movil.setMaterialEje(dto.getMaterialEje());
            entidad = movil;
        } else {
            entidad = new GrupoElectrogeno();
        }

        entidad.setCodigo(dto.getCodigo());
        entidad.setVidaUtil(dto.getVidaUtil());
        entidad.setTipoCombustible(dto.getTipoCombustible());
        entidad.setTipoArranque(dto.getTipoArranque());
        entidad.setPMin(dto.getPMin());
        entidad.setPMax(dto.getPMax());
        entidad.setInsonorizado(dto.getInsonorizado());
        entidad.setCapo(dto.getCapo());
        return entidad;
    }

    private GrupoElectrogenoResponseDTO mapToResponseDTO(GrupoElectrogeno entidad) {
        GrupoElectrogenoResponseDTO.GrupoElectrogenoResponseDTOBuilder builder = GrupoElectrogenoResponseDTO.builder()
                .id(entidad.getId())
                .codigo(entidad.getCodigo())
                .vidaUtil(entidad.getVidaUtil())
                .tipoCombustible(entidad.getTipoCombustible())
                .tipoArranque(entidad.getTipoArranque())
                .pMin(entidad.getPMin())
                .pMax(entidad.getPMax())
                .insonorizado(entidad.getInsonorizado())
                .capo(entidad.getCapo())
                .potenciaMedia((entidad.getPMin() + entidad.getPMax()) / 2.0)
                .precioVentaCalculado(calcularPrecioVenta(entidad));

        if (entidad instanceof GrupoElectrogenoMovil movil) {
            builder.tipoGrupo("Móvil")
                    .cantidadRuedas(movil.getCantidadRuedas())
                    .materialEje(movil.getMaterialEje());
        } else {
            builder.tipoGrupo("Fijo");
        }

        return builder.build();
    }
}
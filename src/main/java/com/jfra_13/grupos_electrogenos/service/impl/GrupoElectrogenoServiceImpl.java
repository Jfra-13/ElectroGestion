package com.jfra_13.grupos_electrogenos.service.impl;

import com.jfra_13.grupos_electrogenos.exception.ResourceNotFoundException;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoResponseDTO;
import com.jfra_13.grupos_electrogenos.model.dto.GrupoMovilResumenDTO;
import com.jfra_13.grupos_electrogenos.model.dto.PaginatedResponseDTO;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogenoMovil;
import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.mapper.GrupoElectrogenoMapper;
import com.jfra_13.grupos_electrogenos.repository.GrupoElectrogenoRepository;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Transactional(readOnly = true)
public class GrupoElectrogenoServiceImpl implements GrupoElectrogenoService {

    private final GrupoElectrogenoRepository repository;
    private final GrupoElectrogenoMapper mapper;

    public GrupoElectrogenoServiceImpl(GrupoElectrogenoRepository repository, GrupoElectrogenoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public GrupoElectrogenoResponseDTO guardarGrupo(GrupoElectrogenoRequestDTO dto) {
        GrupoElectrogeno entidad = mapper.toEntity(dto);
        GrupoElectrogeno guardado = repository.save(entidad);
        return mapper.toResponse(guardado, calcularPrecioVenta(guardado));
    }

    @Override
    public GrupoElectrogenoResponseDTO obtenerPorId(Long id) {
        GrupoElectrogeno entidad = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo Electrógeno no encontrado con ID: " + id));
        return mapper.toResponse(entidad, calcularPrecioVenta(entidad));
    }

    @Override
    @Transactional
    public GrupoElectrogenoResponseDTO actualizarGrupo(Long id, GrupoElectrogenoRequestDTO dto) {
        GrupoElectrogeno existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo Electrógeno no encontrado con ID: " + id));

        mapper.updateEntity(dto, existing);

        GrupoElectrogeno actualizado = repository.save(existing);
        return mapper.toResponse(actualizado, calcularPrecioVenta(actualizado));
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
        return repository.findByTipoCombustibleOrderByPMaxDesc(combustible, Pageable.unpaged()).getContent().stream()
                .map(grupo -> mapper.toResponse(grupo, calcularPrecioVenta(grupo)))
                .collect(Collectors.toList());
    }

    @Override
    public List<GrupoMovilResumenDTO> buscarMovilesPorEje(MaterialEje material) {
        List<GrupoElectrogenoMovil> moviles = repository.buscarMovilesAutomaticosPorEje(material, Pageable.unpaged()).getContent();
        return moviles.stream()
                .map(mapper::toResumen)
                .collect(Collectors.toList());
    }

    @Override
    public PaginatedResponseDTO<GrupoElectrogenoResponseDTO> buscarPorCombustiblePaginado(TipoCombustible combustible, Pageable pageable) {
        Page<GrupoElectrogeno> page = repository.findByTipoCombustibleOrderByPMaxDesc(combustible, pageable);
        List<GrupoElectrogenoResponseDTO> content = page.stream()
                .map(grupo -> mapper.toResponse(grupo, calcularPrecioVenta(grupo)))
                .collect(Collectors.toList());

        return new PaginatedResponseDTO<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public PaginatedResponseDTO<GrupoMovilResumenDTO> buscarMovilesPorEjePaginado(MaterialEje material, Pageable pageable) {
        Page<GrupoElectrogenoMovil> page = repository.buscarMovilesAutomaticosPorEje(material, pageable);
        List<GrupoMovilResumenDTO> content = page.stream()
                .map(mapper::toResumen)
                .collect(Collectors.toList());

        return new PaginatedResponseDTO<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

}
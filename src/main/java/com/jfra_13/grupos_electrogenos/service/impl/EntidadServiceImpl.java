package com.jfra_13.grupos_electrogenos.service.impl;

import com.jfra_13.grupos_electrogenos.exception.DuplicateResourceException;
import com.jfra_13.grupos_electrogenos.model.dto.EntidadRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.EntidadResponseDTO;
import com.jfra_13.grupos_electrogenos.model.entity.Entidad;
import com.jfra_13.grupos_electrogenos.repository.EntidadRepository;
import com.jfra_13.grupos_electrogenos.service.EntidadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EntidadServiceImpl implements EntidadService {

    private final EntidadRepository entidadRepository;

    public EntidadServiceImpl(EntidadRepository entidadRepository) {
        this.entidadRepository = entidadRepository;
    }

    @Override
    @Transactional
    public EntidadResponseDTO crear(EntidadRequestDTO dto) {
        String nombre = dto.getNombre().trim();
        entidadRepository.findByNombreIgnoreCase(nombre).ifPresent(e -> {
            throw new DuplicateResourceException("Ya existe un cliente con el nombre '" + nombre + "'.");
        });

        Entidad entidad = new Entidad();
        entidad.setNombre(nombre);
        return toDto(entidadRepository.save(entidad));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntidadResponseDTO> buscar(String nombre) {
        List<Entidad> entidades = (nombre == null || nombre.isBlank())
                ? entidadRepository.findAll(org.springframework.data.domain.Sort.by("nombre").ascending())
                : entidadRepository.findByNombreContainingIgnoreCaseOrderByNombreAsc(nombre.trim());
        return entidades.stream().map(this::toDto).collect(Collectors.toList());
    }

    private EntidadResponseDTO toDto(Entidad e) {
        return new EntidadResponseDTO(e.getId(), e.getNombre(), e.getCreatedAt());
    }
}

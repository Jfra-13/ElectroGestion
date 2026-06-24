package com.jfra_13.grupos_electrogenos.repository;

import com.jfra_13.grupos_electrogenos.model.entity.Entidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntidadRepository extends JpaRepository<Entidad, Long> {

    /** Autocomplete: coincidencia parcial case-insensitive, orden alfabético. */
    List<Entidad> findByNombreContainingIgnoreCaseOrderByNombreAsc(String nombre);

    /** Chequeo de duplicado al crear. */
    Optional<Entidad> findByNombreIgnoreCase(String nombre);
}

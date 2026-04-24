package com.jfra_13.grupos_electrogenos.repository;

import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogenoMovil;
import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrupoElectrogenoRepository extends JpaRepository<GrupoElectrogeno, Long> {

    // RF04: Listar por combustible, ordenado por potencia máxima descendente
    // Spring Boot lee el nombre del método y arma el SQL "SELECT * WHERE tipoCombustible = ? ORDER BY pMax DESC"
    List<GrupoElectrogeno> findByTipoCombustibleOrderByPMaxDesc(TipoCombustible tipoCombustible);

    // RF07: Listar móviles automáticos según el material del eje
    // Como GrupoElectrogenoMovil hereda de GrupoElectrogeno, usamos JPQL para buscar específicamente en la clase hija
    @Query("SELECT m FROM GrupoElectrogenoMovil m WHERE m.tipoArranque = 'AUTOMATICO' AND m.materialEje = :material")
    List<GrupoElectrogenoMovil> buscarMovilesAutomaticosPorEje(@Param("material") MaterialEje material);
}
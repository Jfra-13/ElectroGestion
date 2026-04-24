package com.jfra_13.grupos_electrogenos.repository;

import com.jfra_13.grupos_electrogenos.model.dto.RankingEntidadDTO;
import com.jfra_13.grupos_electrogenos.model.dto.ReportePagoDTO;
import com.jfra_13.grupos_electrogenos.model.entity.SolicitudCompra;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudCompraRepository extends JpaRepository<SolicitudCompra, Long> {

    // RF05: Ranking de entidades ordenadas por la cantidad total de grupos solicitados
    @Query("SELECT s.entidad.nombre AS nombreEntidad, SUM(s.cantidad) AS totalSolicitados " +
            "FROM SolicitudCompra s GROUP BY s.entidad.nombre ORDER BY totalSolicitados DESC")
    List<RankingEntidadDTO> obtenerRankingEntidades();

    // RF08: Listado por tipo de pago, ordenado por cantidad
    @Query("SELECT s.nombreSolicitante AS solicitante, s.cantidad AS cantidad " +
            "FROM SolicitudCompra s WHERE s.tipoPago = :tipoPago ORDER BY s.cantidad DESC")
    List<ReportePagoDTO> obtenerReportePorTipoPago(@Param("tipoPago") TipoPago tipoPago);
}
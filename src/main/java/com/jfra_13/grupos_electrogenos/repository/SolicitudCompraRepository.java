package com.jfra_13.grupos_electrogenos.repository;

import com.jfra_13.grupos_electrogenos.model.dto.RankingEntidadDTO;
import com.jfra_13.grupos_electrogenos.model.dto.RankingVendedorDTO;
import com.jfra_13.grupos_electrogenos.model.dto.ReportePagoDTO;
import com.jfra_13.grupos_electrogenos.model.entity.SolicitudCompra;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Variante paginada para listar ventas filtradas por tipo de pago
    Page<SolicitudCompra> findByTipoPago(TipoPago tipoPago, Pageable pageable);

    // Ventas de un vendedor: las ve el propio empleado (sus ventas) y el admin
    // cuando filtra por un empleado concreto.
    Page<SolicitudCompra> findByVendedorId(Long vendedorId, Pageable pageable);

    // Ranking de ventas por empleado para la vista del jefe (solo ventas con vendedor).
    @Query("SELECT s.vendedor.username AS vendedor, COUNT(s) AS cantidadVentas, SUM(s.total) AS totalRecaudado " +
            "FROM SolicitudCompra s WHERE s.vendedor IS NOT NULL " +
            "GROUP BY s.vendedor.username ORDER BY totalRecaudado DESC")
    List<RankingVendedorDTO> obtenerRankingVendedores();
}
package com.jfra_13.grupos_electrogenos.model.dto;

/** Ranking de ventas por empleado para la vista del jefe (ADMIN). */
public interface RankingVendedorDTO {
    String getVendedor();          // username del vendedor
    Long getCantidadVentas();      // COUNT devuelve Long en JPQL
    Double getTotalRecaudado();    // SUM de los totales congelados
}

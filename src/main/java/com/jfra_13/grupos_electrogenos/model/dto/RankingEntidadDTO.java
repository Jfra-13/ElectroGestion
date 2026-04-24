package com.jfra_13.grupos_electrogenos.model.dto;

public interface RankingEntidadDTO {
    String getNombreEntidad();
    Long getTotalSolicitados(); // SUM devuelve Long en JPQL
}
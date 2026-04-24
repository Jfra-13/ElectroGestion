package com.jfra_13.grupos_electrogenos.model.entity;

import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "solicitudes_compra")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String identificador;

    private String nombreSolicitante;

    @Enumerated(EnumType.STRING)
    private TipoPago tipoPago;

    private Integer cantidad;
    private Double potenciaRequerida;

    @Enumerated(EnumType.STRING)
    private TipoCombustible tipoCombustible;

    private Integer vidaUtilSolicitada;

    // Relación: Muchas solicitudes pueden pertenecer a una misma Entidad
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entidad_id", nullable = false)
    private Entidad entidad;
}
package com.jfra_13.grupos_electrogenos.model.entity;

import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_compra", indexes = {
        @Index(name = "idx_solicitud_identificador", columnList = "identificador"),
        @Index(name = "idx_solicitud_entidad", columnList = "entidad_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotNull
    private String identificador;

    @NotNull
    private String nombreSolicitante;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TipoPago tipoPago;

    @Min(1)
    @NotNull
    private Integer cantidad;

    @DecimalMin("0.0")
    @NotNull
    private Double potenciaRequerida;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TipoCombustible tipoCombustible;

    @Min(1)
    @NotNull
    private Integer vidaUtilSolicitada;

    // Precio unitario CONGELADO al momento de la venta. No se recalcula desde el
    // grupo en cada lectura: así editar un grupo no altera la recaudación histórica.
    @Column(nullable = false, updatable = false)
    private Double precioUnitario;

    // Total de la venta (precioUnitario * cantidad) persistido al crear/editar.
    @Column(nullable = false)
    private Double total;

    // Relación: Muchas solicitudes pueden pertenecer a una misma Entidad
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entidad_id", nullable = false)
    @NotNull
    private Entidad entidad;

    // Relación: Una solicitud vende un modelo específico de Grupo Electrógeno
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    @NotNull
    private GrupoElectrogeno grupoElectrogeno;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
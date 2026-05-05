package com.jfra_13.grupos_electrogenos.model.entity;

import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
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
@Table(name = "grupos_electrogenos", indexes = {
        @Index(name = "idx_grupo_combustible", columnList = "tipoCombustible")
})
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrupoElectrogeno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotNull
    private String codigo;

    @Min(1)
    @NotNull
    private Integer vidaUtil;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TipoCombustible tipoCombustible;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TipoArranque tipoArranque;

    @DecimalMin("0.0")
    @NotNull
    private Double pMin;

    @DecimalMin("0.0")
    @NotNull
    private Double pMax;

    private Boolean insonorizado;
    private Boolean capo;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @AssertTrue(message = "pMin debe ser menor que pMax")
    public boolean isPotenciaValida() {
        if (pMin == null || pMax == null) return true;
        return pMin < pMax;
    }
}
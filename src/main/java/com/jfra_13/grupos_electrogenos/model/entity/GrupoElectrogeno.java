package com.jfra_13.grupos_electrogenos.model.entity;

import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "grupos_electrogenos")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrupoElectrogeno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;

    private Integer vidaUtil;

    @Enumerated(EnumType.STRING)
    private TipoCombustible tipoCombustible;

    @Enumerated(EnumType.STRING)
    private TipoArranque tipoArranque;

    private Double pMin;
    private Double pMax;
    private Boolean insonorizado;
    private Boolean capo;
}
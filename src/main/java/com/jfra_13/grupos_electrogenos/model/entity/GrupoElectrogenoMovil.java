package com.jfra_13.grupos_electrogenos.model.entity;

import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "grupos_electrogenos_moviles")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class GrupoElectrogenoMovil extends GrupoElectrogeno {

    private Integer cantidadRuedas;

    @Enumerated(EnumType.STRING)
    private MaterialEje materialEje;
}
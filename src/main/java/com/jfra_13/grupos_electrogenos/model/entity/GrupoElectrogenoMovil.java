package com.jfra_13.grupos_electrogenos.model.entity;

import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "grupos_electrogenos_moviles", indexes = {
        @Index(name = "idx_grupo_movil_material", columnList = "materialEje")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class GrupoElectrogenoMovil extends GrupoElectrogeno {

    @Min(1)
    @NotNull
    private Integer cantidadRuedas;

    @Enumerated(EnumType.STRING)
    @NotNull
    private MaterialEje materialEje;
}
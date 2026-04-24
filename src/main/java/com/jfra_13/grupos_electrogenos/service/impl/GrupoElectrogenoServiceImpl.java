package com.jfra_13.grupos_electrogenos.service.impl;

import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogenoMovil;
import com.jfra_13.grupos_electrogenos.model.enums.MaterialEje;
import com.jfra_13.grupos_electrogenos.model.enums.TipoArranque;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.repository.GrupoElectrogenoRepository;
import com.jfra_13.grupos_electrogenos.service.GrupoElectrogenoService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GrupoElectrogenoServiceImpl implements GrupoElectrogenoService {

    private final GrupoElectrogenoRepository repository;

    // Inyección de dependencias por constructor (Buena práctica)
    public GrupoElectrogenoServiceImpl(GrupoElectrogenoRepository repository) {
        this.repository = repository;
    }

    @Override
    public GrupoElectrogeno guardarGrupo(GrupoElectrogeno grupo) {
        return repository.save(grupo);
    }

    @Override
    public Double calcularPrecioVenta(GrupoElectrogeno grupo) {
        if (grupo == null || grupo.getVidaUtil() == null || grupo.getPMin() == null || grupo.getPMax() == null) {
            throw new IllegalArgumentException("Faltan datos básicos para calcular el precio.");
        }

        // 1. Cálculo base: años de vida útil * potencia media
        double potenciaMedia = (grupo.getPMin() + grupo.getPMax()) / 2.0;
        double precio = grupo.getVidaUtil() * potenciaMedia;

        // 2. Modificador Acústico: +10 si es insonorizado Y tiene capó
        if (Boolean.TRUE.equals(grupo.getInsonorizado()) && Boolean.TRUE.equals(grupo.getCapo())) {
            precio += 10.0;
        }

        // 3. Modificador de Arranque: +15 si es automático
        if (TipoArranque.AUTOMATICO.equals(grupo.getTipoArranque())) {
            precio += 15.0;
        }

        // 4. Modificador de Valor Agregado (Fijo vs Móvil)
        if (grupo instanceof GrupoElectrogenoMovil movil) {
            // Regla para Móviles
            double valorAgregado = movil.getCantidadRuedas() * 5.0;
            if (MaterialEje.ACERO.equals(movil.getMaterialEje())) {
                valorAgregado += 20.0;
            } else if (MaterialEje.ALEACION.equals(movil.getMaterialEje())) {
                valorAgregado += 13.0;
            }
            precio += valorAgregado;
        } else {
            // Regla para Fijos: Constante de 200
            precio += 200.0;
        }

        return precio;
    }

    @Override
    public List<GrupoElectrogeno> buscarPorCombustible(TipoCombustible combustible) {
        return repository.findByTipoCombustibleOrderByPMaxDesc(combustible);
    }

    @Override
    public List<com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoResponseDTO> buscarMovilesPorEje(MaterialEje material) {
        List<GrupoElectrogenoMovil> moviles = repository.buscarMovilesAutomaticosPorEje(material);

        // El requerimiento RF07 pide devolver SOLO código y vida útil.
        // Mapeamos las entidades al DTO usando el patrón Builder que configuramos con Lombok.
        return moviles.stream().map(movil ->
                com.jfra_13.grupos_electrogenos.model.dto.GrupoElectrogenoResponseDTO.builder()
                        .codigo(movil.getCodigo())
                        .vidaUtil(movil.getVidaUtil())
                        // Omitimos el resto de datos para cumplir con el filtro estricto
                        .build()
        ).collect(Collectors.toList());
    }
}
package com.jfra_13.grupos_electrogenos.service;

import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraResponseDTO;
import com.jfra_13.grupos_electrogenos.model.entity.Entidad;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.SolicitudCompra;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import com.jfra_13.grupos_electrogenos.repository.EntidadRepository;
import com.jfra_13.grupos_electrogenos.repository.GrupoElectrogenoRepository;
import com.jfra_13.grupos_electrogenos.repository.SolicitudCompraRepository;
import com.jfra_13.grupos_electrogenos.service.impl.SolicitudCompraServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SolicitudCompraServiceTest {

    @Mock
    private SolicitudCompraRepository repository;
    @Mock
    private GrupoElectrogenoService grupoService;
    @Mock
    private EntidadRepository entidadRepository;
    @Mock
    private GrupoElectrogenoRepository grupoRepository;

    @InjectMocks
    private SolicitudCompraServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void debeActualizarGrupoCuandoCambiaPotenciaRequerida() {
        // Arrange
        Long id = 1L;
        Entidad entidad = new Entidad();
        entidad.setId(10L);
        entidad.setNombre("Entidad Test");

        GrupoElectrogeno grupoViejo = new GrupoElectrogeno();
        grupoViejo.setId(100L);
        grupoViejo.setPMax(200.0);
        grupoViejo.setTipoCombustible(TipoCombustible.GASOIL);

        GrupoElectrogeno grupoNuevo = new GrupoElectrogeno();
        grupoNuevo.setId(200L);
        grupoNuevo.setPMax(500.0);
        grupoNuevo.setTipoCombustible(TipoCombustible.GASOIL);

        SolicitudCompra existing = new SolicitudCompra();
        existing.setId(id);
        existing.setTipoCombustible(TipoCombustible.GASOIL);
        existing.setPotenciaRequerida(150.0);
        existing.setGrupoElectrogeno(grupoViejo);
        existing.setEntidad(entidad);

        SolicitudCompraRequestDTO dto = new SolicitudCompraRequestDTO();
        dto.setEntidadId(10L);
        dto.setTipoCombustible(TipoCombustible.GASOIL);
        dto.setPotenciaRequerida(400.0); // Aumenta potencia
        dto.setCantidad(1);
        dto.setNombreSolicitante("Juan");
        dto.setTipoPago(TipoPago.EFECTIVO);
        dto.setVidaUtilSolicitada(5);

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(entidadRepository.findById(10L)).thenReturn(Optional.of(entidad));
        when(grupoRepository.findByTipoCombustibleOrderByPMaxDesc(TipoCombustible.GASOIL))
                .thenReturn(Collections.singletonList(grupoNuevo));
        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(grupoService.calcularPrecioVenta(any())).thenReturn(1000.0);

        // Act
        SolicitudCompraResponseDTO response = service.actualizarSolicitud(id, dto);

        // Assert
        assertNotNull(response);
        assertEquals(200L, response.getGrupoId());
        verify(grupoRepository, times(1)).findByTipoCombustibleOrderByPMaxDesc(TipoCombustible.GASOIL);
    }
}

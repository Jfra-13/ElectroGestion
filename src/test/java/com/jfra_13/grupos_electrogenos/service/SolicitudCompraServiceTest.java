package com.jfra_13.grupos_electrogenos.service;

import com.jfra_13.grupos_electrogenos.exception.ResourceNotFoundException;
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
    void debeCalcularIngresosTotalesCorrectamente() {
        // Arrange
        GrupoElectrogeno g1 = new GrupoElectrogeno();
        g1.setVidaUtil(10);
        g1.setPMin(100.0);
        g1.setPMax(200.0);

        SolicitudCompra s1 = new SolicitudCompra();
        s1.setCantidad(2);
        s1.setGrupoElectrogeno(g1);

        when(repository.findAll()).thenReturn(Collections.singletonList(s1));
        when(grupoService.calcularPrecioVenta(g1)).thenReturn(1000.0);

        // Act
        Double total = service.calcularIngresosTotales();

        // Assert
        assertEquals(2000.0, total);
        verify(repository).findAll();
        verify(grupoService).calcularPrecioVenta(g1);
    }

    @Test
    void debeLanzarExcepcionCuandoNoHayGrupoDisponible() {
        // Arrange
        SolicitudCompraRequestDTO dto = new SolicitudCompraRequestDTO();
        dto.setEntidadId(1L);
        dto.setPotenciaRequerida(1000.0);
        dto.setTipoCombustible(TipoCombustible.GASOIL);

        Entidad entidad = new Entidad();
        entidad.setId(1L);

        when(entidadRepository.findById(1L)).thenReturn(Optional.of(entidad));
        when(grupoRepository.findByTipoCombustibleOrderByPMaxDesc(TipoCombustible.GASOIL))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            service.crearSolicitud(dto);
        });
    }
}

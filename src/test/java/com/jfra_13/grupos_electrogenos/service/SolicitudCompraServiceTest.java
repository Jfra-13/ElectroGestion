package com.jfra_13.grupos_electrogenos.service;

import com.jfra_13.grupos_electrogenos.exception.ResourceNotFoundException;
import com.jfra_13.grupos_electrogenos.model.dto.SolicitudCompraRequestDTO;
import com.jfra_13.grupos_electrogenos.model.entity.Entidad;
import com.jfra_13.grupos_electrogenos.model.entity.GrupoElectrogeno;
import com.jfra_13.grupos_electrogenos.model.entity.SolicitudCompra;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.mapper.SolicitudCompraMapper;
import com.jfra_13.grupos_electrogenos.repository.EntidadRepository;
import com.jfra_13.grupos_electrogenos.repository.GrupoElectrogenoRepository;
import com.jfra_13.grupos_electrogenos.repository.SolicitudCompraRepository;
import com.jfra_13.grupos_electrogenos.service.impl.SolicitudCompraServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SolicitudCompraServiceTest {

    @Mock
    private SolicitudCompraRepository repository;
    @Mock
    private GrupoElectrogenoService grupoService;
    @Mock
    private EntidadRepository entidadRepository;
    @Mock
    private GrupoElectrogenoRepository grupoRepository;

    private SolicitudCompraServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SolicitudCompraServiceImpl(repository, grupoService, entidadRepository, grupoRepository,
                Mappers.getMapper(SolicitudCompraMapper.class));
    }

    @Test
    void debeCalcularIngresosTotalesDesdeTotalesCongelados() {
        // I4: los ingresos suman el total CONGELADO de cada venta, NO recalculan
        // desde el grupo actual (editar el grupo no debe alterar la recaudación).
        SolicitudCompra s1 = new SolicitudCompra();
        s1.setCantidad(2);
        s1.setPrecioUnitario(1000.0);
        s1.setTotal(2000.0);

        when(repository.findAll()).thenReturn(Collections.singletonList(s1));

        // Act
        Double total = service.calcularIngresosTotales();

        // Assert
        assertEquals(2000.0, total);
        verify(repository).findAll();
        verifyNoInteractions(grupoService);
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
        when(grupoRepository.findByTipoCombustibleOrderByPMaxDesc(TipoCombustible.GASOIL, org.springframework.data.domain.Pageable.unpaged()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(Collections.emptyList()));

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(ResourceNotFoundException.class, () -> service.crearSolicitud(dto));
    }
}

package com.jfra_13.grupos_electrogenos.model.dto;

import com.jfra_13.grupos_electrogenos.model.enums.EstadoVenta;
import com.jfra_13.grupos_electrogenos.model.enums.TipoCombustible;
import com.jfra_13.grupos_electrogenos.model.enums.TipoPago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCompraResponseDTO {
    private Long id;
    private String identificador;
    private String nombreSolicitante;
    private TipoPago tipoPago;
    private Integer cantidad;
    private Double potenciaRequerida;
    private TipoCombustible tipoCombustible;
    private Integer vidaUtilSolicitada;
    private Long entidadId;
    private String entidadNombre;
    private Long grupoId;
    private String grupoCodigo;
    // Precio unitario congelado al momento de la venta (no se recalcula desde el grupo).
    private Double precioVentaUnitario;
    // Total de la venta (precioVentaUnitario * cantidad), también congelado.
    private Double total;
    // Vendedor que registró la venta (null en ventas legacy sin dueño).
    private Long vendedorId;
    private String vendedorUsername;
    // Estado y auditoría de anulación. Una venta ANULADA conserva su monto: lo que
    // la saca de la caja es el estado, no borrar el número. Campos null si ACTIVA.
    private EstadoVenta estado;
    private String motivoAnulacion;
    private LocalDateTime anuladaAt;
    private String anuladaPor;          // username de quién anuló
}

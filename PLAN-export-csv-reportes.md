# Plan: Exportación CSV de ventas con filtros (fecha, cliente, vendedor)

## Objetivo

Endpoint que exporta ventas (`SolicitudCompra`) a **CSV** filtrando por **rango de
fechas**, **cliente** (`entidadId`) y **vendedor** (`vendedorId`). Pensado para
volcar los datos a herramientas de análisis (Excel / Power BI / Looker) sin
depender de la UI: un único archivo plano, importable, con todas las columnas
crudas de la venta.

**Por qué CSV y no PDF:** el objetivo es *trabajo de datos*, no un documento de
presentación. CSV se importa directo a Power BI / Excel para pivotar, graficar y
cruzar. PDF sería un callejón sin salida para análisis y agrega una dependencia
(iText/OpenPDF) que hoy no está en el `pom.xml`. Si más adelante se necesita un
reporte "lindo" para imprimir, se hace en la capa de BI, no en el backend.

## Alcance

- Solo lectura. No toca creación/edición/borrado de ventas.
- Backend genera el CSV; el frontend solo dispara la descarga (link / fetch + blob).
- Un solo endpoint con filtros opcionales que se combinan (AND).
- Sin dependencias nuevas. CSV se arma a mano (formato simple, escape de comillas).

## Estado actual (verificado)

- `SolicitudCompra` **ya tiene `createdAt`** (`@CreationTimestamp`, `LocalDateTime`) y `updatedAt`. Sirve como fecha de la venta.
- `entidad_id` y `vendedor_id` **ya están indexados**. `created_at` **NO**.
- Campos legacy nullable: `precioUnitario`, `total`, `vendedor` (ventas previas a esas columnas). El CSV debe tolerar nulls.
- Endpoints actuales solo devuelven JSON (ranking-clientes, reporte-pagos, por-empleado, ingresos-totales). Ninguno filtra por fecha ni exporta.

## Diseño

### Endpoint

```
GET /api/v1/ventas/export.csv
    ?desde=2026-01-01T00:00:00      (opcional, LocalDateTime ISO)
    &hasta=2026-12-31T23:59:59      (opcional)
    &entidadId=5                    (opcional, cliente)
    &vendedorId=3                   (opcional)
```

- `@PreAuthorize("hasRole('ADMIN')")` — mismo criterio que los demás reportes.
- Respuesta: `Content-Type: text/csv; charset=UTF-8`,
  `Content-Disposition: attachment; filename="ventas_<timestamp>.csv"`.
- Si no se pasa ningún filtro → exporta todo (con cuidado, ver "Riesgos").
- **Un solo endpoint con rango cubre mes/semana/día.** El front calcula
  `desde`/`hasta` (inicio/fin de mes, semana, día). No se hacen endpoints
  separados por granularidad.

### Query (repository)

Una query con filtros opcionales vía nulls. Patrón ya usado en el repo (`@Query` + `@Param`):

```java
@Query("SELECT s FROM SolicitudCompra s " +
       "WHERE (:desde IS NULL OR s.createdAt >= :desde) " +
       "AND (:hasta IS NULL OR s.createdAt <= :hasta) " +
       "AND (:entidadId IS NULL OR s.entidad.id = :entidadId) " +
       "AND (:vendedorId IS NULL OR s.vendedor.id = :vendedorId) " +
       "ORDER BY s.createdAt ASC")
List<SolicitudCompra> buscarParaExport(@Param("desde") LocalDateTime desde,
                                       @Param("hasta") LocalDateTime hasta,
                                       @Param("entidadId") Long entidadId,
                                       @Param("vendedorId") Long vendedorId);
```

> Alternativa si la tabla crece mucho: `Specification` (Criteria) para que el
> filtro sea dinámico y no evalúe condiciones muertas. Por ahora el `IS NULL OR`
> es suficiente y más simple. `// ponytail:` query estática; pasar a Specification si el volumen lo pide.

### Servicio

```java
void exportarVentasCsv(LocalDateTime desde, LocalDateTime hasta,
                       Long entidadId, Long vendedorId, Writer out);
```

- Recibe el `Writer` del response y escribe línea a línea (streaming).
- Escapa campos: si un valor contiene `,`, `"` o salto de línea → envolver en comillas y duplicar comillas internas.
- Nulls → celda vacía.

### Columnas CSV (orden propuesto)

```
id,identificador,createdAt,nombreSolicitante,entidad,vendedor,
tipoPago,tipoCombustible,cantidad,potenciaRequerida,vidaUtilSolicitada,
precioUnitario,total
```

- `entidad` → `entidad.nombre`; `vendedor` → `vendedor.username` (o vacío si null).
- Fechas en ISO-8601 para que Power BI/Excel las parseen sin fricción.

### Controller

- Toma el `HttpServletResponse`, setea headers, delega en el servicio pasándole `response.getWriter()`.
- `@RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) LocalDateTime desde` (idem hasta).

## Streaming (rendimiento)

- Escribir directo al `Writer` del response → no se materializa toda la lista en memoria como String gigante.
- Para volúmenes grandes, evaluar `StreamingResponseBody` + cursor/paginado del repo. **No** en la primera versión: `List` + escritura directa alcanza para el volumen actual. `// ponytail:` carga en List; pasar a stream paginado si el export se vuelve lento o OOM.

## Migración / Índice

- Agregar índice en `created_at` (la columna que ahora ordena y filtra el rango):
  - Vía entity: añadir `@Index(name = "idx_solicitud_created_at", columnList = "created_at")` en `@Table`.
  - Vía Flyway: nueva migración `V5__index_solicitud_created_at.sql` con `CREATE INDEX ...`.
  - **Las dos deben coincidir** (el proyecto usa `ddl-auto=validate` en prod + Flyway). El índice real lo crea Flyway; el `@Index` es para que el `validate` no se queje y para dev.

## Tests (regla de oro del proyecto)

- Integración (`@SpringBootTest` + MockMvc):
  1. Export sin filtros → 200, `Content-Type text/csv`, header de cantidad de filas esperada.
  2. Filtro por rango de fechas → solo las ventas dentro del rango.
  3. Filtro por `entidadId` → solo de ese cliente.
  4. Filtro por `vendedorId` → solo de ese vendedor.
  5. Filtros combinados (fecha + vendedor).
  6. No-ADMIN (EMPLEADO) → 403.
- Unit del escapeo CSV: valor con coma, con comillas, con null → salida correcta.

## Riesgos / Decisiones abiertas

1. **Export sin filtros = toda la tabla.** ¿Permitirlo o exigir al menos rango de fechas? Recomendado: permitir, pero documentarlo. Si preocupa, tope de filas o `desde` obligatorio.
2. **Zona horaria.** `createdAt` es `LocalDateTime` (sin TZ). El front manda fechas en la misma zona del server. Aclararlo en el doc de API.
3. **`vendedorId`/`entidadId` inexistentes** → CSV vacío (no error). ¿OK? Recomendado: sí, 200 con solo header.
4. **Separador.** Coma estándar. Excel-ES a veces espera `;`. Si el cliente usa Excel local, evaluar `;` o dejarlo configurable por query param. Por defecto: coma (Power BI lo maneja bien).

## Checklist de implementación

- [ ] `SolicitudCompraRepository.buscarParaExport(...)`
- [ ] `SolicitudCompraService.exportarVentasCsv(...)` + impl (escapeo + escritura)
- [ ] `SolicitudCompraController` → `GET /export.csv` con headers y `@PreAuthorize ADMIN`
- [ ] `@Index` en entity + migración Flyway `V5` para `created_at`
- [ ] Tests integración (6) + unit de escapeo
- [ ] Actualizar `API-FRONTEND.md` con el nuevo endpoint y params
- [ ] Verificar suite verde contra Postgres real

## Esfuerzo estimado

~2-3 h incluyendo tests. Cero dependencias nuevas.

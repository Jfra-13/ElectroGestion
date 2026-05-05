# Backlog de Optimización de Rendimiento

## 1. Base de Datos
- **Índices adicionales**: 
    - Crear índice compuesto en `solicitudes_compra(entidad_id, cantidad)` para optimizar el ranking de clientes.
    - Índice en `usuarios(username)` (ya existe por @Column(unique=true), verificar en producción real).
- **Paginación**:
    - Implementar `Pageable` en `GrupoElectrogenoRepository.findByTipoCombustibleOrderByPMaxDesc` para evitar cargar miles de registros en memoria.
- **Pool de Conexiones**:
    - Ajustar `spring.datasource.hikari.maximum-pool-size` y `minimum-idle` según carga esperada en `application-prod.properties`.

## 2. Caching
- **Cache de Catálogo**:
    - Implementar `@Cacheable` (Caffeine o Redis) para `/api/v1/grupos-electrogenos/filtro/*`. Los grupos no cambian con tanta frecuencia como las ventas.
- **Cache de Ranking**:
    - Cachear el resultado de `obtenerRankingClientes` por un tiempo breve (TTL 5-10 min) para reducir el costo de la agregación SQL.

## 3. Consultas y DTOs
- **Proyecciones JPA**:
    - Utilizar interfaces de proyección en lugar de entidades completas para el ranking y reportes de pago para reducir el tráfico de red entre la DB y el App Server.
- **Fetch Joins**:
    - Asegurar que `SolicitudCompra` cargue `Entidad` y `GrupoElectrogeno` mediante `JOIN FETCH` en consultas personalizadas para evitar el problema N+1.

## 4. Procesamiento Asíncrono
- **Generación de Reportes**:
    - Si se añaden reportes pesados (PDF/Excel), utilizar `@Async` para no bloquear el hilo de ejecución de la solicitud web.

## 5. Monitoreo
- **Actuator & Prometheus**:
    - Exponer métricas para monitorear el tiempo de respuesta de los endpoints y el estado del pool de conexiones.

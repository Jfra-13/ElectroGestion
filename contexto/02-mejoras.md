# 02 — Mejoras recomendadas (a tu criterio)

> Estas son mejoras que **elevan** el proyecto, pero NO son bloqueantes. Vos decidís cuáles
> entran según hacia dónde quieras llevar el producto. Ordenadas por impacto/esfuerzo.
> Las cosas que SÍ o SÍ hay que hacer están en `03-indispensables.md`.

---

## A. Modelo de dominio y reglas de negocio

- **Sacar los "números mágicos" del cálculo de precio.** Hoy el precio vive como constantes
  hardcodeadas en `GrupoElectrogenoServiceImpl.calcularPrecioVenta` (`+10`, `+15`, `+200`,
  `+20`, `+13`, `*5`). Nadie sabe de dónde salen ni puede auditarlas. Moverlas a configuración
  (`application.properties` / tabla de parámetros) o a un objeto de política de precios.
- **Lógica de selección del grupo en una venta.** `crearSolicitud` ordena por `pMax DESC` y
  toma el primero que cumple la potencia → elige el equipo **más grande/caro** que sirve.
  ¿Es la intención? Lo normal sería el más chico que cumple (más rentable para el cliente).
  Definí la regla de forma explícita.
- **Ciclo de vida de la venta.** Hoy una venta nace y queda. Agregar estados
  (`PENDIENTE → CONFIRMADA → ENTREGADA → CANCELADA`) le da realismo y trazabilidad.
- **CRUD de clientes (`Entidad`).** Podés crear ventas que referencian `entidadId`, pero no hay
  endpoint para crear/editar/listar clientes; solo se siembran por SQL. Falta esa gestión.
- **Vincular la venta a un usuario** (`createdBy`) para saber quién la registró (auditoría).

## B. Reportes y consultas

- **Filtros por rango de fechas** en `ingresos-totales`, `ranking-clientes` y `reporte-pagos`.
  Un reporte financiero sin "desde/hasta" sirve de poco en un negocio.
- **Agregaciones en SQL, no en memoria.** Mover `calcularIngresosTotales` a un `@Query` con
  `SUM(...)` en vez de `findAll().stream()`.
- **Proyecciones JPA** para rankings/reportes (ya está anotado en `PERFORMANCE_BACKLOG.md`).
- **`JOIN FETCH`** en las consultas de `SolicitudCompra` para evitar N+1 sobre `Entidad` y
  `GrupoElectrogeno`.

## C. Seguridad (lo que mejora, más allá de lo indispensable)

- **Refresh tokens + logout/blacklist.** Hoy es un JWT de 1h sin renovación ni invalidación.
  Para una app de uso diario conviene refresh token y revocación.
- **Rate limiting en `/login`** para frenar fuerza bruta (Bucket4j o filtro propio).
- **Respuestas de auth consistentes.** `register` devuelve un `String` y status 200; `login`
  devuelve un DTO. Unificar (p. ej. `register` → 201 con DTO).
- **Política de contraseñas** (longitud mínima, validación en `RegisterRequestDTO`).

## D. Operación y despliegue

- **Migraciones con Flyway o Liquibase.** Reemplazar `ddl-auto` y los SQL sueltos. Es el
  estándar para versionar el esquema. (Tiene un pie en `03` porque sin esto prod no crea tablas.)
- **Dockerfile + docker-compose** (app + PostgreSQL) para levantar el entorno reproducible.
- **Spring Boot Actuator + métricas** (health, readiness, Prometheus). Ya está en el backlog.
- **Pipeline CI** (GitHub Actions): build + test + cobertura en cada push.
- **Pool de conexiones Hikari** afinado en prod (`maximum-pool-size`, `minimum-idle`).

## E. Calidad de código y repo

- **Limpiar el repositorio.** Sacar de git: `grupos-electrogenos.zip`, `grupo.json`,
  `data/testdb.mv.db`, `data/testdb.trace.db`, y cualquier JAR. Agregarlos al `.gitignore`.
- **Subir la cobertura real** y reconciliar la contradicción 50% (pom) vs 80% (README). Que el
  número documentado sea el verdadero, y que los tests cubran reglas de negocio, no solo el
  happy path.
- **Tests de integración con base efímera** (Testcontainers PostgreSQL) en vez de la H2 en
  archivo compartida (`application-test.properties`), que es fuente de tests frágiles.
- **Revisar `spring.main.allow-bean-definition-overriding=true`.** Suele tapar un problema de
  beans duplicados. Si no hace falta, sacarlo; si hace falta, documentar por qué.
- **Caching** del catálogo y del ranking (Caffeine/Redis), ya identificado en el backlog.
- **Logging estructurado** (niveles, correlación de request) para diagnóstico en prod.

## F. Caching (del propio backlog, vale la pena)

- `@Cacheable` en `/grupos-electrogenos/filtro/*` — el catálogo cambia poco.
- Cache con TTL corto (5–10 min) para `obtenerRankingClientes`.

---

### Cómo priorizar si tuviera que elegir 5

1. Migraciones (Flyway) — sin esto no hay prod real.
2. Congelar precio/total de la venta — integridad contable (también en `03`).
3. Filtros por fecha en reportes — valor de negocio inmediato.
4. CRUD de clientes — falta funcional visible.
5. Docker + CI — para trabajar en serio y desplegar sin dolor.

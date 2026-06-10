# 01 — Opinión técnica del backend

> Veredicto honesto sobre el estado real del proyecto `grupos-electrogenos`.
> Fecha de revisión: 2026-06-06. Stack: Java 21, Spring Boot 3.4.3.

## TL;DR

Es un backend **bien construido como proyecto de aprendizaje / portfolio**: arquitectura
limpia, convenciones correctas de Spring, buen uso de DTOs, MapStruct, validación y Swagger.

Pero **NO está "PRODUCCIÓN READY"** como afirman `PROYECTO_COMPLETADO.md` y `RESUMEN_FINAL.txt`.
Hay fallos críticos de seguridad y de integridad de datos que, para un negocio que maneja
ventas y dinero, son bloqueantes. La etiqueta de "100% funcional y listo para producción" es
optimista: funciona en demo, no aguanta un entorno real.

Resumen en una línea: **excelente como MVP/práctica, riesgoso como producto real sin cerrar las brechas del README 03.**

---

## Qué está bien (y bien hecho)

- **Arquitectura por capas correcta y consistente.** Controller → Service (interfaz + impl) →
  Repository → Entity, con DTOs en los bordes. No hay filtración de entidades JPA hacia la API.
  Esto es lo que separa un proyecto serio de un script.
- **Patrón interfaz/implementación en servicios.** `GrupoElectrogenoService` + `...ServiceImpl`.
  Bien para testear y para invertir dependencias.
- **MapStruct para mapeo.** Cero boilerplate manual de mapeo, y resolvés bien la herencia
  (`GrupoElectrogeno` vs `GrupoElectrogenoMovil`) con `@ObjectFactory` y `@AfterMapping`. Eso
  está pensado, no copiado.
- **Validación declarativa** con Bean Validation en DTOs y entidades, incluido un `@AssertTrue`
  de regla de negocio (`pMin < pMax`). Muy bien.
- **Manejo centralizado de errores** con `@ControllerAdvice` y respuestas de error uniformes.
- **Herencia JOINED** para el grupo móvil: modelado relacional correcto, sin desnormalizar.
- **Separación de perfiles** dev/test/prod, con `ddl-auto=validate` en prod (no `update`, que
  sería un error grave). Buen criterio.
- **Paginación** implementada en los listados.
- **Documentación OpenAPI/Swagger** con anotaciones por endpoint.
- **Gate de cobertura con JaCoCo** y conciencia de performance (`PERFORMANCE_BACKLOG.md`).

Para tu nivel, esto demuestra que entendés Spring de verdad, no solo que copiaste un tutorial.

---

## Qué está mal (lo que rompe el "listo para producción")

### Seguridad (lo más urgente)
1. **Escalada de privilegios en `/register` público.** El endpoint está bajo
   `/api/v1/auth/**` con `permitAll()` (`SecurityConfig.java:47`) y el `RegisterRequestDTO`
   acepta un campo `roles` que se asigna tal cual (`AuthController.java:83-86`). Cualquiera
   puede registrarse mandando `{"roles":["ROLE_ADMIN"]}` y volverse administrador. Esto es
   crítico: anula toda la autorización por roles.
2. **Datos financieros expuestos sin autenticación.** Todos los GET son públicos, incluidos
   `GET /api/v1/ventas/ingresos-totales` (recaudación total del negocio),
   `/ventas/ranking-clientes` y `/ventas/reporte-pagos` (`SecurityConfig.java:49-51`).
   Cualquier persona en internet puede leer tu facturación y tu cartera de clientes.
3. **Secretos con valores por defecto en el repo.** `application.properties:9` trae un
   `jwt.secret` por defecto, y `application-prod.properties:4` define `DB_PASSWORD:admin` como
   default de producción. Defaults de secretos en git = no es seguro.

### Integridad de datos / negocio
4. **El precio de la venta NO se congela.** `SolicitudCompra` no guarda el precio total ni el
   precio unitario; se recalcula dinámicamente desde el grupo en cada lectura
   (`SolicitudCompraServiceImpl.java:126`, `SolicitudCompraResponseDTO.precioVentaUnitario`).
   Si mañana editás el `pMin/pMax/vidaUtil` de un grupo, **cambia retroactivamente la
   recaudación de todas las ventas históricas**. Para contabilidad eso es inaceptable.
5. **El stock es decorativo.** Existe el campo `stock` y un PATCH para tocarlo, pero crear una
   venta (`crearSolicitud`) nunca descuenta inventario ni valida disponibilidad. Un sistema de
   ventas que no mueve stock no está terminado funcionalmente.
6. **Producción no puede arrancar usable.** `DataInitializer` solo corre en dev/test
   (`DataInitializer.java:15`), así que en prod no se siembran roles ni admin. Con
   `ddl-auto=validate` y sin migraciones, **el esquema ni siquiera se crea**, y aunque
   existiera, no habría `ROLE_ADMIN` ni forma de crear el primer admin. "Prod ready" no se
   sostiene.

### Otros (no bloqueantes pero reales)
7. **`calcularIngresosTotales` carga todas las ventas en memoria** y recalcula precio fila por
   fila (`findAll().stream()`), con N+1 sobre el grupo lazy. No escala.
8. **Sin bloqueo optimista (`@Version`)** en ninguna entidad → riesgo de lost update en stock.
9. **Sin handler genérico de `Exception`** → un error inesperado filtra stack trace (500 por
   defecto de Spring).
10. **Inconsistencias de documentación:** el README dice gate de cobertura 80% pero el `pom.xml`
    exige 50%; los docs de cierre celebran 50% como "listo". El número real es bajo.
11. **Basura versionada:** `grupos-electrogenos.zip`, `grupo.json`, y la DB H2 de test
    (`data/testdb.mv.db`) están en git.

---

## Veredicto final

| Dimensión | Estado |
|---|---|
| Arquitectura y código | 🟢 Sólido, por encima del promedio |
| Patrones y convenciones | 🟢 Correctos |
| Seguridad | 🔴 Crítico (escalada de roles + datos financieros públicos) |
| Integridad de datos | 🔴 Crítico (precio no congelado, stock no se mueve) |
| Operabilidad en prod | 🔴 No arranca usable (sin migraciones, sin bootstrap) |
| Testing | 🟡 Existe, pero cobertura/realismo bajos |
| Documentación | 🟡 Buena pero con afirmaciones falsas ("prod ready") |

**No está terminado para un negocio real.** Está terminado como demostración técnica. La
diferencia está en el README `03-indispensables.md`: hasta cerrar esos puntos, no lo pondría
a manejar plata de nadie. Lo del README `02-mejoras.md` es tu decisión y tu producto.

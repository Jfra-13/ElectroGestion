# Plan de acción — Cierre de indispensables para Producción

> Plan ejecutable por fases para resolver los 8 puntos bloqueantes de
> [`contexto/03-indispensables.md`](contexto/03-indispensables.md). Mientras estos puntos sigan
> abiertos, la etiqueta "PRODUCCIÓN READY" es falsa.
>
> Contexto de soporte: [`01-opinion.md`](contexto/01-opinion.md) (veredicto) y
> [`02-mejoras.md`](contexto/02-mejoras.md) (mejoras opcionales, fuera de este plan).
>
> **Regla de oro:** ninguna fase se considera cerrada sin que su *criterio de aceptación* esté
> verificado con un test (o, donde no aplique, con una prueba manual reproducible).

---

## Orden y dependencias

```
Fase 1  Seguridad de acceso        I1, I2, I3   (sin dependencias — empezar aquí)
   │
Fase 2  Esquema y arranque         I6, I7       (I7 depende de I6)
   │
Fase 3  Integridad contable        I4, I5       (añaden columnas → requieren Flyway de Fase 2)
   │
Fase 4  Robustez                   I8           (independiente — cierra el plan)
```

**Por qué este orden:**
1. **Seguridad primero** porque I1/I2 son explotables hoy mismo con un solo POST/GET y anulan toda la autorización. Máximo riesgo, cero dependencias.
2. **Migraciones antes que dominio** porque I4 e I5 agregan columnas/cambian el esquema: sin Flyway (I6) no hay forma versionada de aplicarlas en prod, y el bootstrap (I7) se siembra como migración.
3. **Robustez al final** porque I8 no bloquea nada de lo anterior y conviene cerrarlo cuando el comportamiento ya es el definitivo.

---

## Fase 1 — Seguridad de acceso (I1, I2, I3)

**Objetivo:** que nadie sin autenticación lea datos del negocio ni se auto-asigne privilegios, y
que en producción la app falle al arrancar si faltan secretos.

### 1.1 — Eliminar la escalada de privilegios en el registro público (I1)
- **Problema:** `/api/v1/auth/register` es público (`SecurityConfig.java:47`) y asigna el campo
  `roles` que manda el cliente (`AuthController.java:79-86`, `RegisterRequestDTO.roles`).
- **Riesgo:** cualquiera se vuelve `ROLE_ADMIN` con un POST.
- **Acciones:**
  - Quitar el campo `roles` de `RegisterRequestDTO`.
  - En `AuthController`/servicio de registro, asignar SIEMPRE `ROLE_USER` (rol mínimo), ignorando
    cualquier input de roles.
  - La creación de administradores se hace solo por canal protegido (ver Fase 2 / I7).
- **Archivos:** `model/dto/RegisterRequestDTO.java`, `controller/AuthController.java`.
- **Criterio de aceptación:** test que registra con `{"roles":["ROLE_ADMIN"]}` y verifica que el
  usuario queda con `ROLE_USER`; el DTO ya no expone `roles`.

### 1.2 — Proteger datos financieros y de clientes (I2)
- **Problema:** todos los GET son públicos, incluidos `ingresos-totales`, `ranking-clientes`,
  `reporte-pagos`, ventas y precios (`SecurityConfig.java:49-51`).
- **Riesgo:** facturación, cartera de clientes y precios expuestos sin autenticar.
- **Acciones:**
  - En `SecurityConfig`, exigir autenticación + rol en todos los endpoints de ventas, reportes y
    recaudación.
  - Como mucho dejar público el catálogo de productos; la información de negocio NO.
  - Definir el rol requerido por endpoint (p. ej. reportes financieros → `ROLE_ADMIN`).
- **Archivos:** `security/SecurityConfig.java`.
- **Criterio de aceptación:** test que llama `GET /ventas/ingresos-totales` sin token → `401/403`;
  con token y rol correcto → `200`; catálogo público sigue `200`.

### 1.3 — Sin secretos por defecto en el repositorio (I3)
- **Problema:** `jwt.secret` con default committeado (`application.properties:9`) y
  `DB_PASSWORD:admin` como default de prod (`application-prod.properties:4`).
- **Riesgo:** si falta una variable de entorno, la app arranca con credenciales públicas conocidas.
- **Acciones:**
  - En el perfil `prod`, referenciar `${JWT_SECRET}` y `${DB_PASSWORD}` **sin valor por defecto**.
  - Si la variable no está en el entorno, la app debe fallar al arrancar (fail-fast), no usar
    default. (Spring: placeholder sin default ya falla; verificar que no haya fallback en `prod`.)
- **Archivos:** `resources/application.properties`, `resources/application-prod.properties`.
- **Criterio de aceptación:** arrancar el perfil `prod` sin `JWT_SECRET`/`DB_PASSWORD` definidos
  produce fallo de arranque (no boot silencioso con default).

**Salida de Fase 1:** registro sin escalada, endpoints de negocio detrás de auth, prod fail-fast.

---

## Fase 2 — Esquema y arranque en producción (I6, I7)

**Objetivo:** que una base limpia de producción pueda crear su esquema y tener roles + primer
admin sin depender del registro público.

### 2.1 — Migraciones de esquema versionadas (I6)
- **Problema:** prod usa `ddl-auto=validate` y `sql.init.mode=never`, sin herramienta de migración:
  no hay quién cree el esquema → la app no arranca en una base limpia.
- **Acciones:**
  - Agregar Flyway (o Liquibase) como dependencia.
  - Crear `src/main/resources/db/migration/V1__baseline.sql` con el esquema completo actual
    (todas las tablas/relaciones que hoy genera JPA).
  - Mantener `ddl-auto=validate` en prod; Flyway crea/evoluciona, JPA solo valida.
- **Archivos:** `pom.xml`, `resources/db/migration/V1__*.sql`, perfiles de properties.
- **Criterio de aceptación:** levantar contra una base vacía → Flyway crea el esquema y la app
  arranca; `validate` pasa.

### 2.2 — Bootstrap de roles y primer administrador (I7)
- **Problema:** `DataInitializer` corre solo en dev/test (`DataInitializer.java:15`): en prod no
  hay roles ni admin inicial.
- **Riesgo:** aunque exista el esquema, no hay `ROLE_ADMIN` ni forma de crear el primer admin.
- **Acciones:**
  - Sembrar los roles (`ROLE_USER`, `ROLE_ADMIN`) vía migración Flyway (`V2__seed_roles.sql`).
  - Crear el primer admin por mecanismo controlado: migración seed que lee credenciales de
    variables de entorno, o comando/endpoint protegido de bootstrap de un solo uso.
  - **No** depender del registro público (ya cerrado en Fase 1).
- **Archivos:** `resources/db/migration/V2__seed_roles.sql` (+ seed admin), revisar
  `security/DataInitializer.java`.
- **Criterio de aceptación:** en una base limpia de prod existen los roles y un `ROLE_ADMIN`
  inicial capaz de autenticarse; las credenciales del admin no están hardcodeadas en el repo.

**Salida de Fase 2:** prod despliega sobre base limpia y queda usable (esquema + admin).

---

## Fase 3 — Integridad contable y de inventario (I4, I5)

**Objetivo:** que los números de ventas pasadas no mientan y que el stock refleje la realidad.

> Estas tareas modifican el esquema → se aplican como nuevas migraciones Flyway de la Fase 2.

### 3.1 — Congelar precio y total al momento de la venta (I4)
- **Problema:** `SolicitudCompra` no persiste precio unitario ni total; se recalcula en cada lectura
  desde el grupo actual (`SolicitudCompraServiceImpl.java:126`).
- **Riesgo:** editar un grupo cambia retroactivamente la recaudación histórica.
- **Acciones:**
  - Agregar columnas inmutables `precioUnitario` y `total` (= unitario × cantidad) a
    `SolicitudCompra` (entidad + migración `V3__solicitud_precio_total.sql`).
  - Al crear la venta, calcular y persistir ambos valores una sola vez.
  - Reportes y DTOs de respuesta leen los valores guardados, **no** recalculan desde el grupo.
  - Definir backfill para filas existentes (si las hay) en la migración.
- **Archivos:** `model/entity/SolicitudCompra.java`,
  `service/impl/SolicitudCompraServiceImpl.java`, `model/dto/SolicitudCompraResponseDTO.java`,
  migración `V3__*.sql`.
- **Criterio de aceptación:** test que crea una venta, luego edita el precio del grupo y verifica
  que el total/unitario de esa venta NO cambió.

### 3.2 — Mover y validar el stock con la venta (I5)
- **Problema:** crear una venta no descuenta stock ni valida disponibilidad; `stock` solo se toca
  por un PATCH manual.
- **Riesgo:** se "venden" unidades inexistentes.
- **Acciones (elegir UNA y dejarla explícita):**
  - **Opción A — gestionar stock:** al crear la venta, validar disponibilidad y descontar stock;
    rechazar la operación si no alcanza. (Considerar `@Version` para evitar lost update, ver
    `02-mejoras.md`.)
  - **Opción B — eliminar el concepto:** quitar `stock` y su PATCH del dominio si el negocio no
    gestiona inventario.
  - Lo inaceptable es dejar un stock que finge gestionar inventario y no lo hace.
- **Archivos:** `service/impl/SolicitudCompraServiceImpl.java`,
  `model/entity/GrupoElectrogeno.java`, controller/DTO según la opción.
- **Criterio de aceptación (Opción A):** test que vende más unidades que el stock disponible →
  operación rechazada; venta válida → stock descontado por la cantidad vendida.

**Salida de Fase 3:** recaudación histórica estable y stock consistente con las ventas.

---

## Fase 4 — Robustez mínima (I8)

**Objetivo:** que ningún error inesperado filtre detalles internos al cliente.

### 4.1 — Handler genérico de excepciones (I8)
- **Problema:** `GlobalExceptionHandler` no captura `Exception` genérica → el 500 por defecto de
  Spring filtra stack trace (rutas, librerías, a veces datos).
- **Acciones:**
  - Agregar `@ExceptionHandler(Exception.class)` que responda un error genérico y controlado
    (sin stack trace).
  - Registrar el detalle completo solo en logs del servidor.
- **Archivos:** `exception/GlobalExceptionHandler.java`.
- **Criterio de aceptación:** test que fuerza una excepción no mapeada → respuesta genérica
  (sin stack trace en el body); el detalle aparece en logs.

**Salida de Fase 4:** errores controlados, sin fuga de internos.

---

## Checklist de cierre ("verdaderamente listo para producción")

```
Fase 1 — Seguridad
[ ] I1  Registro público no asigna roles; admin solo por canal protegido
[ ] I2  Endpoints financieros/clientes detrás de auth + rol
[ ] I3  Sin defaults de JWT secret ni DB password en prod (fail-fast)

Fase 2 — Esquema y arranque
[ ] I6  Migraciones Flyway/Liquibase creando el esquema
[ ] I7  Roles + primer admin sembrados en prod

Fase 3 — Integridad de datos
[ ] I4  Venta guarda precio unitario y total inmutables
[ ] I5  Stock se descuenta/valida en la venta (o se elimina el concepto)

Fase 4 — Robustez
[ ] I8  Handler genérico de Exception sin filtrar stack traces
```

Cuando las 8 casillas estén marcadas y verificadas con test, recién ahí la etiqueta
"PRODUCCIÓN READY" deja de ser falsa. Lo de [`02-mejoras.md`](contexto/02-mejoras.md) es producto y
decisión tuya — no entra en este plan.

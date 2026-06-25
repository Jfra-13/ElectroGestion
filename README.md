# Sistema de Gestión de Grupos Electrógenos

API REST para la gestión de inventario, ventas y reportes financieros de grupos
electrógenos. Backend construido con **Spring Boot 3.4 / Java 21**, con seguridad
basada en **JWT**, persistencia **JPA/PostgreSQL**, migraciones versionadas con
**Flyway** y documentación viva con **OpenAPI/Swagger**.

> Este README documenta la **arquitectura, estructura y decisiones técnicas** del
> proyecto. Para el contrato de cada endpoint (request/response JSON, permisos,
> params) ver **[`API-FRONTEND.md`](APIs-Backend.md)**.

---

## Tabla de contenidos

1. [Dominio del problema](#1-dominio-del-problema)
2. [Stack tecnológico](#2-stack-tecnológico)
3. [Arquitectura aplicada](#3-arquitectura-aplicada)
4. [Estructura de carpetas y archivos](#4-estructura-de-carpetas-y-archivos)
5. [Modelo de datos](#5-modelo-de-datos)
6. [Reglas de negocio clave](#6-reglas-de-negocio-clave)
7. [Seguridad](#7-seguridad)
8. [Manejo de errores](#8-manejo-de-errores)
9. [Perfiles y configuración](#9-perfiles-y-configuración)
10. [Migraciones de base de datos](#10-migraciones-de-base-de-datos)
11. [Testing y cobertura](#11-testing-y-cobertura)
12. [Cómo ejecutar](#12-cómo-ejecutar)
13. [Decisiones técnicas destacadas](#13-decisiones-técnicas-destacadas)

---

## 1. Dominio del problema

El sistema modela la venta de **grupos electrógenos** (generadores eléctricos). Hay
dos tipos: **fijos** y **móviles** (estos últimos con ruedas y material de eje). El
negocio expone un **catálogo** público de productos y una operatoria privada de
**ventas** que valida stock, calcula precio y genera reportes financieros (ranking de
clientes, reporte por tipo de pago, ingresos totales).

Entidades centrales:

- **GrupoElectrógeno** / **GrupoElectrógenoMóvil** — el producto (modelo + stock).
- **SolicitudCompra** ("venta") — registro de una venta concreta.
- **Entidad** — el cliente (empresa) que compra.
- **Usuario** / **Role** — autenticación y autorización.

---

## 2. Stack tecnológico

| Capa | Tecnología |
|------|------------|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.4.3 |
| Web | Spring Web (MVC, REST) |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL (prod) · H2 en memoria (dev/test) |
| Migraciones | Flyway (solo prod sobre PostgreSQL) |
| Seguridad | Spring Security + JJWT 0.11.5 (HS256) |
| Mapeo DTO↔Entidad | MapStruct 1.6.3 |
| Boilerplate | Lombok |
| Validación | Bean Validation (Jakarta) |
| Documentación API | springdoc-openapi (Swagger UI) 2.8.5 |
| Cobertura | JaCoCo 0.8.12 |
| Testing de integración | JUnit 5 + Spring Test + Testcontainers (PostgreSQL real) |

Build: **Maven** (con wrapper `./mvnw`, no requiere Maven instalado).

---

## 3. Arquitectura aplicada

### Arquitectura en capas (layered / N-tier)

El backend sigue una separación estricta de responsabilidades. El flujo de una
petición es siempre el mismo:

```
HTTP request
   │
   ▼
Controller  ──►  Service (interfaz)  ──►  ServiceImpl (lógica de negocio)
(REST, DTOs)        │                          │
                    │                          ▼
                    │                     Repository (Spring Data JPA)
                    │                          │
                    ▼                          ▼
                  Mapper (MapStruct)        Base de datos
                    │
                    ▼
            DTO de respuesta  ──►  HTTP response (JSON)
```

**Regla de oro del proyecto:** las entidades JPA **nunca** cruzan la frontera HTTP.
El cliente solo ve DTOs. Esto desacopla el modelo de persistencia del contrato de la
API: se puede cambiar la tabla sin romper el frontend, y se evita exponer campos
internos (`version`, timestamps, relaciones lazy).

### Patrones e ideas implementadas

| Patrón / técnica | Dónde | Para qué |
|------------------|-------|----------|
| **Layered architecture** | controller / service / repository | Separación de responsabilidades |
| **DTO pattern** | `model/dto/*` | Desacoplar API de entidades JPA |
| **Mapper pattern** | `mapper/*` (MapStruct) | Conversión declarativa DTO↔entidad |
| **Programación contra interfaces** | `service/*` + `service/impl/*` | Inversión de dependencias (DIP) |
| **Inyección por constructor** | todos los componentes | Inmutabilidad y testabilidad |
| **Repository pattern** | `repository/*` | Abstracción de acceso a datos |
| **Herencia JPA (JOINED)** | `GrupoElectrogeno` → `GrupoElectrogenoMovil` | Modelar especialización con tablas separadas |
| **Strategy de perfiles** | `application-{dev,prod,test}` | Comportamiento por entorno |
| **Bloqueo optimista** | `@Version` en `GrupoElectrogeno` | Concurrencia segura al descontar stock |
| **Global exception handling** | `@ControllerAdvice` | Errores consistentes y sin fugas |

---

## 4. Estructura de carpetas y archivos

```
src/main/java/com/jfra_13/grupos_electrogenos/
├── GruposElectrogenosApplication.java   # Punto de entrada Spring Boot
│
├── config/
│   └── OpenApiConfig.java               # Configura Swagger + esquema de seguridad bearer JWT
│
├── controller/                          # Capa REST. Solo orquesta: valida, delega, responde
│   ├── AuthController.java              # /auth/login y /auth/register
│   ├── GrupoElectrogenoController.java  # CRUD + filtros + cotización de catálogo
│   └── SolicitudCompraController.java   # CRUD de ventas + reportes financieros
│
├── service/                             # Contratos de negocio (interfaces)
│   ├── GrupoElectrogenoService.java
│   ├── SolicitudCompraService.java
│   └── impl/                            # Implementaciones (la lógica real vive acá)
│       ├── GrupoElectrogenoServiceImpl.java
│       └── SolicitudCompraServiceImpl.java
│
├── repository/                          # Spring Data JPA (interfaces, sin implementación manual)
│   ├── GrupoElectrogenoRepository.java  # Queries JPQL custom (filtro por combustible / eje)
│   ├── SolicitudCompraRepository.java   # Reportes con projections (ranking, por pago)
│   ├── EntidadRepository.java
│   ├── UsuarioRepository.java
│   └── RoleRepository.java
│
├── mapper/                              # MapStruct: DTO ↔ entidad
│   ├── GrupoElectrogenoMapper.java      # Maneja la herencia fijo/móvil + campos calculados
│   └── SolicitudCompraMapper.java
│
├── model/
│   ├── entity/                          # Entidades JPA (tablas)
│   │   ├── GrupoElectrogeno.java        # Producto base
│   │   ├── GrupoElectrogenoMovil.java   # Subclase (herencia JOINED)
│   │   ├── SolicitudCompra.java         # Venta
│   │   ├── Entidad.java                 # Cliente
│   │   ├── Usuario.java / Role.java     # Seguridad (ManyToMany)
│   ├── dto/                             # Objetos de transferencia (request/response)
│   │   ├── *RequestDTO / *ResponseDTO
│   │   ├── PaginatedResponseDTO.java    # Envoltorio de paginación genérico
│   │   ├── RankingEntidadDTO / ReportePagoDTO  # Projections de reportes
│   │   └── AuthResponseDTO / LoginRequestDTO / RegisterRequestDTO
│   └── enums/                           # Valores cerrados del dominio
│       ├── TipoCombustible.java         # NAFTA, GAS_NATURAL, GASOIL
│       ├── TipoArranque.java            # AUTOMATICO, MANUAL
│       ├── MaterialEje.java             # ACERO, ALEACION
│       └── TipoPago.java                # CHEQUE, EFECTIVO
│
├── security/                            # Toda la infraestructura de autenticación/autorización
│   ├── SecurityConfig.java              # Filter chain, CORS, reglas de acceso por ruta
│   ├── JwtUtil.java                     # Genera/valida/parsea tokens JWT (HS256)
│   ├── JwtAuthorizationFilter.java      # Filtro por request: lee Bearer y puebla el contexto
│   ├── JpaUserDetailsService.java       # Carga usuario+roles desde la BD para Spring Security
│   ├── DataInitializer.java             # Seed de roles + admin (solo dev/test)
│   └── AdminBootstrap.java              # Seed del admin en prod (credenciales del entorno)
│
└── exception/
    ├── GlobalExceptionHandler.java      # @ControllerAdvice: traduce excepciones a HTTP
    ├── ResourceNotFoundException.java   # → 404
    └── StockInsuficienteException.java  # → 409

src/main/resources/
├── application.properties               # Común (puerto 8082, perfil dev por defecto, JWT)
├── application-dev.properties           # H2 en memoria, ddl-auto=update, datos de demo
├── application-prod.properties          # PostgreSQL, ddl-auto=validate, Flyway ON, fail-fast
├── application-test.properties          # H2 create-drop efímero
├── data-dev.sql                         # Seed RICO de dev (perfil dev): roles, 2 vendedores, 9 grupos por código, 2 clientes y 8 ventas con precio REAL calculado. Ver §9.1
├── data-entidades.sql                   # Seed MÍNIMO de test (perfil test): 1 entidad "Empresa Demo" idempotente
└── db/migration/                        # Migraciones Flyway (V1..V4) — solo prod

src/test/java/...                        # 21 clases de test (unit + integración + Testcontainers)
```

---

## 5. Modelo de datos

### Herencia: fijo vs. móvil

`GrupoElectrogeno` usa `@Inheritance(strategy = JOINED)`. Esto crea **dos tablas**:

- `grupos_electrogenos` — campos comunes (código, potencias, combustible, stock…).
- `grupos_electrogenos_moviles` — solo los campos extra del móvil (`cantidad_ruedas`,
  `material_eje`), unidas por PK/FK compartida (`fk_movil_grupo`).

Ventaja de JOINED: sin columnas nulas "basura" (a diferencia de SINGLE_TABLE) y
normalización limpia. Costo: un JOIN al leer móviles (asumido, el volumen es bajo).

### Entidades y relaciones

```
Usuario  ──< usuarios_roles >──  Role          (ManyToMany, EAGER)

Entidad  1 ───< N  SolicitudCompra  N >─── 1  GrupoElectrogeno
                       (venta)                  (producto)
```

- `SolicitudCompra → Entidad` y `SolicitudCompra → GrupoElectrogeno`: ambas
  `@ManyToOne(fetch = LAZY)` para no traer datos que el caso de uso no pide.
- Índices declarados en las entidades: por `tipoCombustible`, por `materialEje`, por
  `identificador` y por `entidad_id` (aceleran los filtros y reportes).

### Campos no obvios

| Campo | Entidad | Por qué existe |
|-------|---------|----------------|
| `version` (`@Version`) | GrupoElectrogeno | Bloqueo optimista al descontar stock |
| `precioUnitario` (`updatable=false`) | SolicitudCompra | Precio **congelado** al vender (ver §6) |
| `total` | SolicitudCompra | Total persistido, no recalculado en cada lectura |
| `createdAt` / `updatedAt` | varias | Auditoría automática (`@CreationTimestamp`/`@UpdateTimestamp`) |
| `identificador` | SolicitudCompra | Código público de venta (UUID de 8 chars) |

---

## 6. Reglas de negocio clave

### 6.1 Cálculo del precio de venta

Implementado en `GrupoElectrogenoServiceImpl.calcularPrecioVenta()`. Es una función
pura sobre el grupo:

```
potenciaMedia = (pMin + pMax) / 2
precio        = vidaUtil × potenciaMedia
              + 10   si (insonorizado AND capó)
              + 15   si arranque AUTOMÁTICO
si es MÓVIL:  + (cantidadRuedas × 5)
              + 20   si eje ACERO     /  + 13 si eje ALEACIÓN
si es FIJO:   + 200
```

### 6.2 Precio y total congelados (integridad histórica)

**Problema que resuelve:** si el precio de cada venta se recalculara desde el grupo en
cada lectura, **editar un grupo cambiaría retroactivamente la recaudación histórica**.

**Solución:** al crear la venta (`crearSolicitud`), se calcula el precio unitario **una
vez** y se persiste junto con el total (`precioUnitario × cantidad`). La columna
`precio_unitario` es `updatable=false`. Los reportes (`calcularIngresosTotales`) suman
los `total` ya congelados, nunca recalculan. Editar un grupo no toca ventas pasadas.

### 6.3 Selección automática de grupo + validación de stock

Al registrar una venta, el sistema **no** recibe un grupo: lo **elige** él. Busca entre
los grupos del combustible pedido (ordenados por `pMax` desc) el primero cuya potencia
máxima cubra la requerida. Luego:

1. Verifica stock disponible ≥ cantidad pedida → si no, lanza
   `StockInsuficienteException` (HTTP **409**).
2. Descuenta el stock dentro de la misma transacción.

### 6.4 Concurrencia: bloqueo optimista

El descuento de stock es vulnerable a *lost updates* (dos ventas simultáneas leen el
mismo stock y ambas descuentan). La columna `@Version` en `GrupoElectrogeno` hace que
Hibernate detecte la escritura concurrente y falle la segunda transacción en vez de
sobrescribir en silencio.

---

## 7. Seguridad

### Modelo: JWT stateless

`SecurityConfig` define una cadena de filtros **sin sesión** (`STATELESS`): cada request
se autentica solo con su token. No hay cookies de sesión ni CSRF (deshabilitado por ser
API stateless con bearer token).

**Flujo:**

1. `POST /api/v1/auth/login` → `AuthController` autentica con `AuthenticationManager` y
   `JwtUtil.generateToken()` firma un JWT **HS256** con `subject = username` y un claim
   `roles`. Expira en 1 hora.
2. En cada request protegido, `JwtAuthorizationFilter` (un `OncePerRequestFilter`) lee
   el header `Authorization: Bearer <token>`, lo valida contra `JpaUserDetailsService`
   y puebla el `SecurityContext`.
3. La autorización fina se aplica por método con `@PreAuthorize("hasRole('ADMIN')")`.

### Reglas de acceso (definidas en `SecurityConfig`)

| Ruta | Acceso |
|------|--------|
| `POST /api/v1/auth/**` | 🔓 Público (login / registro) |
| `/swagger-ui/**`, `/v3/api-docs/**` | 🔓 Público |
| `GET /api/v1/grupos-electrogenos/**` | 🔓 Público (catálogo) |
| Resto de `/grupos-electrogenos/**` (POST/PUT/DELETE/PATCH) | 🔒 `ROLE_ADMIN` |
| `GET /api/v1/ventas`, `GET /api/v1/ventas/{id}` | 🔐 Autenticado (cualquier rol) |
| Resto de `/ventas/**` (crear/editar/borrar + reportes) | 🔒 `ROLE_ADMIN` |

> **Defensa en profundidad:** las ventas no son públicas a nivel de filtro
> (`anyRequest().authenticated()`), y los reportes financieros además exigen ADMIN por
> `@PreAuthorize`. Dos capas, no una.

### Decisiones de seguridad

- **Registro siempre asigna `ROLE_USER`.** `AuthController.register()` nunca acepta un
  rol del cliente: la creación de administradores no pasa por el endpoint público.
- **Contraseñas con BCrypt** (`PasswordEncoder`), nunca en texto plano.
- **Bootstrap del admin separado por entorno:**
  - `DataInitializer` (`@Profile("dev","test")`) → crea `admin/admin123` para
    desarrollo.
  - `AdminBootstrap` (`@Profile("prod")`) → crea el admin desde variables de entorno
    (`ADMIN_USERNAME`/`ADMIN_PASSWORD`); **sin defaults → si faltan, el arranque falla
    (fail-fast).** Las credenciales de dev no existen en prod.

---

## 8. Manejo de errores

`GlobalExceptionHandler` (`@ControllerAdvice`) centraliza la traducción de excepciones a
respuestas HTTP consistentes. Nada de stack traces ni `try/catch` repartidos por los
controllers.

| Excepción | HTTP | Forma de respuesta |
|-----------|------|--------------------|
| `MethodArgumentNotValidException` | 400 | Mapa plano `campo → mensaje` |
| `IllegalArgumentException` | 400 | Estándar `{timestamp, status, error, message}` |
| `HttpMessageNotReadableException` | 400 | Enum / JSON inválido |
| `ResourceNotFoundException` | 404 | Estándar |
| `StockInsuficienteException` | 409 | Estándar |
| `DataIntegrityViolationException` | 409 | Duplicado / restricción violada |
| `AccessDeniedException` | 403 | "Acceso denegado…" |
| `AuthenticationException` | 401 | Error de autenticación |
| `Exception` (genérico) | 500 | Mensaje genérico; **el detalle solo se loguea en el servidor** |

El handler genérico (500) es la pieza de endurecimiento final: cualquier excepción no
prevista devuelve un mensaje neutro al cliente y registra la causa completa con SLF4J
del lado del servidor. **No se filtran** rutas internas, librerías ni datos.

---

## 9. Perfiles y configuración

Tres perfiles, una estrategia por entorno:

| | **dev** | **test** | **prod** |
|---|---------|----------|----------|
| Base de datos | H2 en memoria | H2 efímero | PostgreSQL |
| `ddl-auto` | `update` | `create-drop` | `validate` |
| Flyway | desactivado | desactivado | **activado** |
| Datos semilla | `data-dev.sql` (seed rico, §9.1) + admin | `data-entidades.sql` (1 cliente) | solo roles (Flyway) + admin bootstrap |
| Ingesta semilla (`spring.sql.init.mode`) | `always` (recargable, §9.1) | `always` | — (Flyway) |
| Secretos | con defaults | con defaults | **sin defaults (fail-fast)** |
| `show-sql` | `true` | `false` | `false` |

**Puerto:** `8082` (definido en `application.properties`, no 8080).

**Por qué `validate` en prod:** en producción JPA **no** modifica el esquema. La única
fuente de verdad del esquema es **Flyway**; JPA solo verifica que las entidades calcen
con las tablas creadas por las migraciones. Si no calzan, no arranca.

**Variables de entorno de prod:** `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`,
`DB_PASSWORD`, `JWT_SECRET_PROD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `ADMIN_EMAIL`,
`ALLOWED_ORIGINS`.

### 9.1 Ingesta de datos de ejemplo (perfil dev / simulaciones)

Para demos y simulaciones, el perfil **dev** carga un seed completo en **cada arranque**.
Como H2 es en memoria y efímera, la base nace vacía en cada `run` y el script la repuebla
idéntica — no hace falta limpiar nada entre simulaciones: **reiniciar = resetear**.

**Qué siembra `src/main/resources/data-dev.sql`:**

| Bloque | Contenido |
|--------|-----------|
| Roles | `ROLE_USER`, `ROLE_ADMIN`, `ROLE_EMPLEADO` |
| Vendedores | `jperez`, `mgomez` (ambos `ROLE_EMPLEADO`, password `password`) |
| Clientes | `Constructora del Sur S.A.` (id 1), `Minera Andes SRL` (id 2) |
| Grupos | **9 grupos por código**: 5 fijos `GE-FIJ-00x` + 4 móviles `GE-MOV-00x` |
| Ventas | 8 ventas con `precio_unitario`/`total` **congelados y reales** (ver abajo) |

> El admin (`admin`/`admin123`) NO está en el script: lo crea `DataInitializer` tras el seed.

**Activar / desactivar** — en `src/main/resources/application-dev.properties`:

```properties
# ACTIVADO (default): recarga el seed en cada arranque dev
spring.sql.init.mode=always
spring.sql.init.data-locations=classpath:data-dev.sql

# DESACTIVAR: poner mode=never (la línea data-locations puede quedar, se ignora)
# spring.sql.init.mode=never
```

⚠️ Con `mode=always` la línea `data-locations` es **obligatoria**: sin ella Spring busca
`data.sql` (que no existe) y el seed no carga. `defer-datasource-initialization=true` hace
que el script corra DESPUÉS de que Hibernate crea el esquema.

**Regla de oro de los precios (NO inventar montos):**
Las ventas del seed **no** llevan precios arbitrarios. Cada `precio_unitario` es la salida
**real** de `GrupoElectrogenoServiceImpl.calcularPrecioVenta()` para el `grupo_id` asignado,
y `total = precio_unitario × cantidad` — exactamente lo que la app congelaría al vender (§6.1,
§6.2). Si se edita un grupo del seed (potencia, vida útil, arranque…), hay que **recalcular**
el precio de sus ventas con la fórmula. Precios reales vigentes por grupo:

| Grupo | Tipo | Precio real | Grupo | Tipo | Precio real |
|-------|------|-------------|-------|------|-------------|
| `GE-FIJ-001` | fijo | 350.0 | `GE-MOV-001` | móvil | 194.5 |
| `GE-FIJ-002` | fijo | 500.0 | `GE-MOV-002` | móvil | 127.5 |
| `GE-FIJ-003` | fijo | 275.0 | `GE-MOV-003` | móvil | 590.0 |
| `GE-FIJ-004` | fijo | 975.0 | `GE-MOV-004` | móvil | 156.0 |
| `GE-FIJ-005` | fijo | 250.0 | | | |

**Comandos para simular** (el wrapper `./mvnw` usa `.mvn/wrapper`, no requiere Maven instalado):

```bash
# Simulación estándar (perfil dev, puerto 8082, seed cargado)
./mvnw spring-boot:run

# Si el 8082 está ocupado por otra instancia: arrancar en puerto libre aleatorio
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=0

# Resetear el estado de la simulación: parar (Ctrl+C) y volver a arrancar.
# H2 es en memoria → cada arranque parte del seed limpio, sin pasos extra.
```

Inspeccionar la base en vivo durante la simulación: consola H2 en
`http://localhost:8082/h2-console` (JDBC URL `jdbc:h2:mem:grupos_electrogenos_dev`, user `sa`,
sin password).

---

## 10. Migraciones de base de datos

Solo se aplican en **prod** (PostgreSQL). Ubicadas en `db/migration/`:

| Versión | Qué hace |
|---------|----------|
| `V1__baseline.sql` | Esquema base completo (tablas, índices, FKs, CHECKs de enums) |
| `V2__seed_roles.sql` | Siembra `ROLE_USER` y `ROLE_ADMIN` (idempotente) |
| `V3__solicitud_precio_total.sql` | Agrega `precio_unitario` y `total` (precio congelado, §6.2) |
| `V4__grupo_version.sql` | Agrega `version` para bloqueo optimista (§6.4) |

Cada migración refleja una regla de negocio del backend. La verificación de que estas
migraciones realmente levantan contra PostgreSQL real corre con **Testcontainers** en
`MigracionesFlywayIntegrationTest`.

---

## 11. Testing y cobertura

21 clases de test cubriendo las capas:

- **Unit** — services, mappers (lógica de negocio aislada).
- **Slice** — controllers (`@WebMvcTest`), repositories (`@DataJpaTest`).
- **Integración** — flujo de compra end-to-end, integridad de venta, seed de entidades,
  seguridad de acceso, CORS, fail-fast de secretos en prod.
- **Testcontainers** — `MigracionesFlywayIntegrationTest` valida las migraciones Flyway
  contra un **PostgreSQL real** en contenedor (no H2), porque las migraciones son
  específicas de PostgreSQL.

**Cobertura (JaCoCo):** el build aplica un *gate* en la fase `verify`. El umbral real
configurado en `pom.xml` es **50% de líneas a nivel BUNDLE** (`COVEREDRATIO ≥ 0.50`);
por debajo de eso el build falla. Reporte HTML en `target/site/jacoco/index.html`.

> ⚠️ Nota: una versión anterior de este README mencionaba un umbral de 80%. El valor
> efectivo que el `pom.xml` enforce hoy es **50%**.

---

## 12. Cómo ejecutar

### Requisitos
- JDK 21
- Maven (incluido vía wrapper `./mvnw`)
- Docker (solo para los tests de Testcontainers)

### Desarrollo (H2, sin instalar nada)
```bash
./mvnw spring-boot:run
```
Arranca en `http://localhost:8082` con perfil `dev`. Admin de demo: `admin` / `admin123`.
Carga automáticamente el seed de ejemplo (9 grupos, clientes y ventas con precios reales);
cómo activarlo/desactivarlo y simular → **[§9.1](#91-ingesta-de-datos-de-ejemplo-perfil-dev--simulaciones)**.

### Tests + cobertura
```bash
./mvnw verify
```
Corre toda la suite y genera el reporte JaCoCo. Falla si la cobertura < 50%.

### Documentación interactiva (Swagger)
- **Swagger UI:** http://localhost:8082/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:8082/v3/api-docs

Para probar endpoints protegidos: hacer `POST /api/v1/auth/login`, copiar el `token`,
pulsar **Authorize** en Swagger y pegarlo.

### Pruebas rápidas (.http)
El archivo `smoke_test.http` permite probar endpoints desde IntelliJ o el REST Client
de VS Code.

---

## 13. Decisiones técnicas destacadas

Resumen de las decisiones que más valor aportan, para defensa del informe:

1. **DTOs + MapStruct en todas las fronteras.** Las entidades JPA nunca se serializan;
   el mapeo es declarativo y verificado en compilación (no reflexión en runtime).
2. **Precio congelado por venta.** Garantiza integridad histórica de la recaudación;
   editar el catálogo no reescribe el pasado.
3. **Bloqueo optimista (`@Version`).** Concurrencia correcta al descontar stock sin
   bloquear filas pesadamente.
4. **Flyway como única fuente del esquema en prod + JPA en `validate`.** Esquema
   versionado, reproducible y auditado; nada de `ddl-auto=update` en producción.
5. **Fail-fast de secretos en prod.** Sin `JWT_SECRET_PROD`, sin credenciales de admin
   → no arranca. Imposible desplegar con defaults inseguros por olvido.
6. **Seguridad en dos capas** (filtro por ruta + `@PreAuthorize` por método) y registro
   público restringido a `ROLE_USER`.
7. **Manejo de errores centralizado** que nunca filtra detalles internos al cliente.
8. **Herencia JPA JOINED** para modelar fijo/móvil sin columnas nulas ni tabla única
   inflada.
9. **Testcontainers** para validar migraciones contra PostgreSQL real, no un sustituto
   en memoria.

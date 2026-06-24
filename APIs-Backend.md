# API Backend — Guía de Integración para Frontend

Documentación técnica completa del backend **Grupos Electrógenos**. Pensada para que
el equipo de frontend conecte sin tener que volver a preguntar al backend: cada
endpoint trae método, ruta, permisos, parámetros, JSON de request y JSON de respuesta.

> Última actualización: refleja el código en `main` (backend prod-ready, 8/8 indispensables
> + gestión de empleados y ventas por vendedor).

---

## 1. Información base

| Dato | Valor |
|------|-------|
| **Base URL (dev local)** | `http://localhost:8082` |
| **Prefijo de todos los endpoints** | `/api/v1` |
| **Formato** | JSON (`Content-Type: application/json`) |
| **Autenticación** | JWT (Bearer token) |
| **Swagger UI** | `http://localhost:8082/swagger-ui/index.html` |
| **OpenAPI JSON** | `http://localhost:8082/v3/api-docs` |

> ⚠️ El puerto es **8082** (definido en `application.properties`, `server.port=8082`).
> Si alguna doc vieja dice 8080, está desactualizada.

### CORS

El backend acepta peticiones con credenciales desde estos orígenes en dev:

```
http://localhost:3000   (React / Next)
http://localhost:4200   (Angular)
http://localhost:4321   (Astro)
```

- Métodos permitidos: `GET, POST, PUT, DELETE, OPTIONS`
- Headers permitidos: `Authorization`, `Content-Type`
- `allowCredentials = true`

En producción el origen se configura con la variable de entorno `ALLOWED_ORIGINS`
(lista separada por comas).

---

## 2. Autenticación (JWT)

### Cómo funciona

1. El front hace `POST /api/v1/auth/login` con usuario y contraseña.
2. El backend responde con un **token JWT**.
3. El front guarda el token y lo manda en **cada** petición protegida en el header:

   ```
   Authorization: Bearer <token>
   ```

4. El token **expira en 1 hora** (`3600000` ms). Pasado ese tiempo, las peticiones
   protegidas fallan y hay que volver a hacer login. No hay refresh token: re-login.

### Credenciales por defecto (solo dev)

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `admin123` | `ROLE_ADMIN` |

En producción el admin se crea por bootstrap con variables de entorno; estas
credenciales NO existen en prod.

### Roles

- `ROLE_USER` — usuario estándar (lo que asigna el registro público `/auth/register`).
  No puede operar ventas ni reportes.
- `ROLE_EMPLEADO` — vendedor. Puede registrar ventas y ver **solo las suyas**. No
  accede a reportes financieros ni a la gestión de usuarios. Se crea solo por el
  canal protegido (`POST /api/v1/usuarios`, lo hace un admin).
- `ROLE_ADMIN` — administrador ("jefe"). Crea/edita/borra, ve todas las ventas,
  filtra por empleado, accede a reportes financieros y crea usuarios.

> El JWT incluye el claim `roles`, así el front puede mostrar/ocultar vistas según
> el rol sin volver a pegarle al backend.

---

## 3. Convenciones generales

### 3.1 Paginación

Los endpoints de listado devuelven un objeto paginado con esta forma:

```json
{
  "content": [ /* array de items */ ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5
}
```

Parámetros de query (todos opcionales, con defaults):

| Param  | Default | Descripción |
|--------|---------|-------------|
| `page` | `0`     | Índice de página (empieza en 0) |
| `size` | `10`    | Tamaño de página |
| `sort` | varía   | `campo,direccion` — ej. `id,desc` o `pMax,asc` |

Ejemplo: `GET /api/v1/grupos-electrogenos?page=0&size=20&sort=pMax,desc`

### 3.2 Formato de errores

**Errores generales** (404, 400 de negocio, 409, 401, 403, 500) — forma estándar:

```json
{
  "timestamp": "2026-06-17T18:30:00.123",
  "status": 404,
  "error": "Not Found",
  "message": "Grupo electrógeno no encontrado con id: 99"
}
```

**Errores de validación** (campos del body inválidos, status `400`) — forma plana
campo→mensaje:

```json
{
  "codigo": "El código es obligatorio",
  "pMax": "La potencia máxima debe ser al menos 1"
}
```

**Error inesperado** (status `500`) — mensaje genérico, sin stack trace ni detalles
internos (el detalle se loguea solo en el servidor):

```json
{
  "timestamp": "2026-06-17T18:30:00.123",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Ocurrió un error inesperado. Intente nuevamente o contacte al administrador."
}
```

### 3.3 Códigos de estado usados

| Código | Significado |
|--------|-------------|
| `200 OK` | Operación exitosa con cuerpo |
| `201 Created` | Recurso creado |
| `204 No Content` | Borrado exitoso (sin cuerpo) |
| `400 Bad Request` | Datos inválidos / formato de enum incorrecto |
| `401 Unauthorized` | Falta token o credenciales inválidas |
| `403 Forbidden` | Autenticado pero sin rol suficiente |
| `404 Not Found` | Recurso inexistente |
| `409 Conflict` | Stock insuficiente o violación de integridad |
| `500 Internal Server Error` | Error inesperado (genérico) |

### 3.4 Enums (valores exactos, sensibles a mayúsculas)

| Enum | Valores válidos |
|------|-----------------|
| `tipoCombustible` | `NAFTA`, `GAS_NATURAL`, `GASOIL` |
| `tipoArranque` | `AUTOMATICO`, `MANUAL` |
| `materialEje` | `ACERO`, `ALEACION` |
| `tipoPago` | `CHEQUE`, `EFECTIVO` |

Mandar un valor fuera de esta lista devuelve `400` con mensaje de formato.

---

## 4. Endpoints

Leyenda de permisos:
- 🔓 **Público** — sin token.
- 🔐 **Autenticado** — requiere token válido (cualquier rol).
- 🟩 **ADMIN o EMPLEADO** — requiere `ROLE_ADMIN` o `ROLE_EMPLEADO`.
- 🔒 **ADMIN** — requiere token con `ROLE_ADMIN`.

### 4.1 Autenticación — `/api/v1/auth`

#### 🔓 POST `/api/v1/auth/login`

Autentica y devuelve el token.

**Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response `200`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTc...",
  "username": "admin",
  "roles": ["ROLE_ADMIN"]
}
```

**Errores:** `401` si las credenciales son inválidas.

---

#### 🔓 POST `/api/v1/auth/register`

Registra un usuario nuevo. **Siempre** asigna `ROLE_USER` (no se puede pedir admin
por aquí, por seguridad).

**Request:**
```json
{
  "username": "operario1",
  "password": "unaClaveSegura",
  "email": "operario1@empresa.com"
}
```

**Response `200`:** texto plano
```
Usuario registrado exitosamente.
```

**Errores:** `400` con texto `Error: El nombre de usuario ya existe.` si el username
ya está tomado.

---

### 4.2 Usuarios (Gestión de empleados) — `/api/v1/usuarios`

> Todo este recurso requiere `ROLE_ADMIN`. Es el canal protegido para que el jefe
> dé de alta empleados/administradores (a diferencia de `/auth/register`, que es
> público y siempre crea `ROLE_USER`).

#### 🔒 POST `/api/v1/usuarios`  *(ADMIN)*

Crea un usuario con el rol elegido.

**Request:**
```json
{
  "username": "vendedor1",
  "password": "claveSegura8+",
  "email": "vendedor1@empresa.com",
  "rol": "EMPLEADO"
}
```

**Campos y validación:**

| Campo | Tipo | Obligatorio | Regla |
|-------|------|-------------|-------|
| `username` | string | ✅ | no vacío, único |
| `password` | string | ✅ | mínimo 8 caracteres |
| `email` | string | ✅ | formato email, único |
| `rol` | enum | ✅ | `EMPLEADO` o `ADMIN` (cualquier otro valor → `400`) |

> El `rol` se manda **sin** el prefijo `ROLE_`. Valores válidos: `EMPLEADO`, `ADMIN`.
> No se puede crear `USER` por acá (ese rol es solo del registro público).

**Response `201`:**
```json
{
  "id": 5,
  "username": "vendedor1",
  "email": "vendedor1@empresa.com",
  "roles": ["ROLE_EMPLEADO"]
}
```

**Errores:** `400` si el username o el email ya existen, o si el rol/datos son
inválidos; `403` sin rol admin.

---

#### 🔒 GET `/api/v1/usuarios`  *(ADMIN)*

Lista todos los usuarios del sistema (para la pantalla de gestión de empleados).

**Response `200`:**
```json
[
  { "id": 1, "username": "admin", "email": "admin@empresa.com", "roles": ["ROLE_ADMIN"] },
  { "id": 5, "username": "vendedor1", "email": "vendedor1@empresa.com", "roles": ["ROLE_EMPLEADO"] }
]
```

**Errores:** `403` sin rol admin. La contraseña **nunca** se devuelve.

---

### 4.3 Grupos Electrógenos — `/api/v1/grupos-electrogenos`

> Los `GET` de este recurso son **públicos** (catálogo). El resto requiere `ROLE_ADMIN`.

#### 🔓 GET `/api/v1/grupos-electrogenos`

Lista paginada de todos los grupos. Sort por defecto: `id,asc`.

**Response `200`:**
```json
{
  "content": [
    {
      "id": 1,
      "codigo": "FJO-001",
      "vidaUtil": 10,
      "tipoCombustible": "GASOIL",
      "tipoArranque": "AUTOMATICO",
      "pMin": 100.0,
      "pMax": 200.0,
      "insonorizado": true,
      "capo": false,
      "potenciaMedia": 150.0,
      "precioVentaCalculado": 1500.0,
      "tipoGrupo": "Fijo",
      "cantidadRuedas": null,
      "materialEje": null,
      "stock": 25
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

---

#### 🔓 GET `/api/v1/grupos-electrogenos/{id}`

Detalle de un grupo.

**Response `200`:** un objeto `GrupoElectrogeno` (misma forma que un item de
`content` arriba).

**Errores:** `404` si no existe.

---

#### 🔓 GET `/api/v1/grupos-electrogenos/{id}/precio`

Cotiza el precio de venta calculado. Devuelve un **número** crudo (no objeto).

**Response `200`:**
```json
1500.0
```

---

#### 🔓 GET `/api/v1/grupos-electrogenos/filtro/combustible`

Filtra por tipo de combustible. Sort por defecto: `pMax,desc`.

**Query params:** `tipo` (enum `tipoCombustible`, **obligatorio**), `page`, `size`, `sort`.

Ejemplo: `GET /api/v1/grupos-electrogenos/filtro/combustible?tipo=GASOIL&page=0&size=10`

**Response `200`:** objeto paginado de grupos (misma forma que el listado).

---

#### 🔓 GET `/api/v1/grupos-electrogenos/filtro/moviles-automaticos`

Lista grupos móviles con arranque automático según material del eje. Devuelve un
**resumen** reducido. Sort por defecto: `codigo,asc`.

**Query params:** `materialEje` (enum, **obligatorio**), `page`, `size`, `sort`.

Ejemplo: `GET /api/v1/grupos-electrogenos/filtro/moviles-automaticos?materialEje=ACERO`

**Response `200`:**
```json
{
  "content": [
    { "codigo": "MOV-010", "vidaUtil": 8 }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

---

#### 🔒 POST `/api/v1/grupos-electrogenos`  *(ADMIN)*

Crea un grupo. Para móviles, mandar `esMovil: true` + campos de móvil.

**Request (grupo fijo):**
```json
{
  "codigo": "FJO-001",
  "vidaUtil": 10,
  "tipoCombustible": "GASOIL",
  "tipoArranque": "AUTOMATICO",
  "pMin": 100.0,
  "pMax": 200.0,
  "insonorizado": true,
  "capo": false,
  "stock": 25,
  "esMovil": false
}
```

**Request (grupo móvil):**
```json
{
  "codigo": "MOV-010",
  "vidaUtil": 8,
  "tipoCombustible": "NAFTA",
  "tipoArranque": "AUTOMATICO",
  "pMin": 50.0,
  "pMax": 120.0,
  "insonorizado": false,
  "capo": true,
  "stock": 10,
  "esMovil": true,
  "cantidadRuedas": 4,
  "materialEje": "ACERO"
}
```

**Campos y validación:**

| Campo | Tipo | Obligatorio | Regla |
|-------|------|-------------|-------|
| `codigo` | string | ✅ | no vacío |
| `vidaUtil` | int | ✅ | ≥ 1 |
| `tipoCombustible` | enum | ✅ | ver enums |
| `tipoArranque` | enum | ✅ | ver enums |
| `pMin` | double | ✅ | ≥ 0 |
| `pMax` | double | ✅ | ≥ 1 |
| `insonorizado` | bool | ❌ | |
| `capo` | bool | ❌ | |
| `stock` | int | ❌ | ≥ 0 |
| `esMovil` | bool | ❌ | marca si es móvil |
| `cantidadRuedas` | int | ❌ | solo móviles |
| `materialEje` | enum | ❌ | solo móviles |

**Response `201`:** el grupo creado (forma `GrupoElectrogenoResponseDTO`).

**Errores:** `400` validación, `403` sin rol admin.

---

#### 🔒 PUT `/api/v1/grupos-electrogenos/{id}`  *(ADMIN)*

Actualiza un grupo existente. Mismo body que POST.

**Response `200`:** grupo actualizado. **Errores:** `404`, `400`, `403`.

> Nota de negocio: editar un grupo **no** cambia el precio de ventas ya registradas;
> el precio de cada venta queda congelado al momento de venderse.

---

#### 🔒 DELETE `/api/v1/grupos-electrogenos/{id}`  *(ADMIN)*

Borra un grupo. **Response `204`** (sin cuerpo). **Errores:** `404`, `403`.

---

#### 🔒 PATCH `/api/v1/grupos-electrogenos/{id}/stock`  *(ADMIN)*

Actualiza el stock disponible. El nuevo stock va como **query param**, no en el body.

`PATCH /api/v1/grupos-electrogenos/1/stock?nuevoStock=50`

**Response `200`:** grupo con el stock actualizado. **Errores:** `404`, `403`.

---

### 4.4 Ventas (Solicitudes de Compra) — `/api/v1/ventas`

> ⚠️ **Ningún endpoint de ventas es público.** Como mínimo requieren token
> (autenticado). Los reportes financieros requieren `ROLE_ADMIN`.
>
> 🧭 **Filtrado por rol:** un `EMPLEADO` solo ve y opera **sus propias** ventas; un
> `ADMIN` ve todas y puede filtrar por empleado. La venta queda atribuida al usuario
> autenticado (campos `vendedorId` / `vendedorUsername` en la respuesta).

#### 🟩 POST `/api/v1/ventas`  *(ADMIN o EMPLEADO)*

Registra una venta. El backend valida y descuenta stock, **congela** el precio
unitario y el total, y **atribuye la venta al usuario autenticado** (no se puede
indicar el vendedor en el request).

**Request:**
```json
{
  "nombreSolicitante": "Juan Pérez",
  "tipoPago": "EFECTIVO",
  "cantidad": 2,
  "potenciaRequerida": 150.0,
  "tipoCombustible": "GASOIL",
  "vidaUtilSolicitada": 10,
  "entidadId": 1
}
```

**Campos y validación:**

| Campo | Tipo | Obligatorio | Regla |
|-------|------|-------------|-------|
| `nombreSolicitante` | string | ✅ | no vacío |
| `tipoPago` | enum | ✅ | `CHEQUE` / `EFECTIVO` |
| `cantidad` | int | ✅ | ≥ 1 |
| `potenciaRequerida` | double | ✅ | ≥ 1 |
| `tipoCombustible` | enum | ✅ | ver enums |
| `vidaUtilSolicitada` | int | ✅ | ≥ 1 |
| `entidadId` | long | ✅ | id de entidad existente (dev: `1` = "Empresa Demo") |

**Response `201`:**
```json
{
  "id": 1,
  "identificador": "VTA-0001",
  "nombreSolicitante": "Juan Pérez",
  "tipoPago": "EFECTIVO",
  "cantidad": 2,
  "potenciaRequerida": 150.0,
  "tipoCombustible": "GASOIL",
  "vidaUtilSolicitada": 10,
  "entidadId": 1,
  "entidadNombre": "Empresa Demo",
  "grupoId": 1,
  "grupoCodigo": "FJO-001",
  "precioVentaUnitario": 1500.0,
  "total": 3000.0,
  "vendedorId": 5,
  "vendedorUsername": "vendedor1"
}
```

> `vendedorId` / `vendedorUsername` identifican quién hizo la venta. En ventas
> legacy (previas a esta función) pueden venir `null`.

**Errores:** `400` validación, `409` si no hay stock suficiente
(`Stock insuficiente...`), `403` sin rol (ni admin ni empleado), `404` si la
entidad/grupo no existe.

---

#### 🔐 GET `/api/v1/ventas`  *(Autenticado — filtra por rol)*

Lista paginada de ventas. Sort por defecto: `id,asc`.

- **EMPLEADO** → ve **solo sus** ventas. Cualquier `vendedorId` que mande se ignora.
- **ADMIN** → ve **todas**, o las de un empleado puntual con el query param opcional
  `vendedorId`.

**Query params:** `page`, `size`, `sort`, y `vendedorId` (long, opcional, **solo
aplica para ADMIN**).

Ejemplo (jefe viendo las ventas del empleado 5):
`GET /api/v1/ventas?vendedorId=5&page=0&size=20`

**Response `200`:** objeto paginado de ventas (forma `SolicitudCompraResponseDTO`,
incluye `vendedorId` / `vendedorUsername`).

---

#### 🔐 GET `/api/v1/ventas/{id}`  *(Autenticado — filtra por rol)*

Detalle de una venta.

- **EMPLEADO** → solo puede ver **su propia** venta. Si pide una ajena, recibe `404`
  (no se revela que existe).
- **ADMIN** → cualquier venta.

**Response `200`:** objeto venta. **Errores:** `404` si no existe, o si un empleado
pide una venta que no es suya.

---

#### 🔒 PUT `/api/v1/ventas/{id}`  *(ADMIN)*

Actualiza una venta. Mismo body que POST. **Response `200`:** venta actualizada.

---

#### 🔒 DELETE `/api/v1/ventas/{id}`  *(ADMIN)*

Borra una venta. **Response `204`** (sin cuerpo).

---

#### 🔒 GET `/api/v1/ventas/por-empleado`  *(ADMIN)*

Ranking de ventas por empleado (vista del jefe): cantidad de ventas y total
recaudado por cada vendedor, ordenado por recaudación descendente. Solo incluye
ventas con vendedor asignado.

**Response `200`:**
```json
[
  { "vendedor": "vendedor1", "cantidadVentas": 8, "totalRecaudado": 24000.0 },
  { "vendedor": "vendedor2", "cantidadVentas": 3, "totalRecaudado": 9000.0 }
]
```

**Errores:** `403` sin rol admin.

---

#### 🔒 GET `/api/v1/ventas/ranking-clientes`  *(ADMIN)*

Ranking de clientes por cantidad solicitada.

**Response `200`:**
```json
[
  { "nombreEntidad": "Empresa Demo", "totalSolicitados": 12 },
  { "nombreEntidad": "Otra S.A.", "totalSolicitados": 5 }
]
```

---

#### 🔒 GET `/api/v1/ventas/reporte-pagos`  *(ADMIN)*

Ventas filtradas por método de pago.

**Query param:** `tipo` (enum `tipoPago`, **obligatorio**).

`GET /api/v1/ventas/reporte-pagos?tipo=EFECTIVO`

**Response `200`:**
```json
[
  { "solicitante": "Juan Pérez", "cantidad": 2 },
  { "solicitante": "María López", "cantidad": 1 }
]
```

---

#### 🔒 GET `/api/v1/ventas/ingresos-totales`  *(ADMIN)*

Suma total recaudada.

**Response `200`:**
```json
{ "totalRecaudado": 12500.0 }
```

---

## 5. Flujo recomendado para el frontend

1. **Login** → guardar `token` (ej. en memoria / `localStorage`).
2. Adjuntar `Authorization: Bearer <token>` en cada request protegido (interceptor).
3. **Catálogo** → los `GET` de grupos son públicos: se pueden mostrar sin login.
4. **Operaciones de admin** (crear/editar/borrar grupos, ventas, reportes) → requieren
   token admin. Si recibís `401`, el token venció → re-login. Si recibís `403`, el
   usuario no es admin.
5. Manejar errores leyendo `message` (errores generales) o el mapa campo→mensaje
   (validación `400`).

### Ejemplo de interceptor (fetch)

```js
async function apiFetch(path, options = {}) {
  const token = localStorage.getItem("token");
  const res = await fetch(`http://localhost:8082${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (res.status === 401) {
    // token vencido o ausente → redirigir a login
  }

  if (!res.ok) {
    const error = await res.json().catch(() => null);
    throw error; // { status, message } o { campo: mensaje }
  }

  return res.status === 204 ? null : res.json();
}
```

---

## 6. Tabla resumen de endpoints

| Método | Ruta | Permiso | Descripción |
|--------|------|---------|-------------|
| POST | `/api/v1/auth/login` | 🔓 | Login → token JWT |
| POST | `/api/v1/auth/register` | 🔓 | Registro público (rol USER) |
| POST | `/api/v1/usuarios` | 🔒 ADMIN | Crear empleado/admin |
| GET | `/api/v1/usuarios` | 🔒 ADMIN | Listar usuarios |
| GET | `/api/v1/grupos-electrogenos` | 🔓 | Listar grupos (paginado) |
| GET | `/api/v1/grupos-electrogenos/{id}` | 🔓 | Detalle de grupo |
| GET | `/api/v1/grupos-electrogenos/{id}/precio` | 🔓 | Cotizar precio (número) |
| GET | `/api/v1/grupos-electrogenos/filtro/combustible` | 🔓 | Filtrar por combustible |
| GET | `/api/v1/grupos-electrogenos/filtro/moviles-automaticos` | 🔓 | Móviles por material de eje |
| POST | `/api/v1/grupos-electrogenos` | 🔒 ADMIN | Crear grupo |
| PUT | `/api/v1/grupos-electrogenos/{id}` | 🔒 ADMIN | Editar grupo |
| DELETE | `/api/v1/grupos-electrogenos/{id}` | 🔒 ADMIN | Borrar grupo |
| PATCH | `/api/v1/grupos-electrogenos/{id}/stock` | 🔒 ADMIN | Actualizar stock |
| POST | `/api/v1/ventas` | 🟩 ADMIN/EMPLEADO | Registrar venta (atribuida al usuario) |
| GET | `/api/v1/ventas` | 🔐 Auth | Listar ventas (empleado: solo suyas; admin: todas o `?vendedorId`) |
| GET | `/api/v1/ventas/{id}` | 🔐 Auth | Detalle de venta (empleado: solo la suya) |
| PUT | `/api/v1/ventas/{id}` | 🔒 ADMIN | Editar venta |
| DELETE | `/api/v1/ventas/{id}` | 🔒 ADMIN | Borrar venta |
| GET | `/api/v1/ventas/por-empleado` | 🔒 ADMIN | Ranking de ventas por empleado |
| GET | `/api/v1/ventas/ranking-clientes` | 🔒 ADMIN | Ranking de clientes |
| GET | `/api/v1/ventas/reporte-pagos` | 🔒 ADMIN | Reporte por tipo de pago |
| GET | `/api/v1/ventas/ingresos-totales` | 🔒 ADMIN | Total recaudado |

---

**Cualquier duda sobre forma de datos: contrastar contra Swagger UI**
(`http://localhost:8082/swagger-ui/index.html`), que es el contrato vivo del backend.

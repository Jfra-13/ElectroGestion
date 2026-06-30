# 📥 Plan de ingesta de datos — Electrogen (producción)

> Cómo poblar la base de **producción** (Supabase) con datos de demo: grupos
> electrógenos, clientes, empleados y ventas. Paso a paso, payloads reales y
> verificación.

---

## 0. Principio rector (LEER PRIMERO)

**La ingesta se hace por la API, NO por SQL crudo.** En especial las **ventas**:

- El backend **calcula y congela** el `precio_unitario` y el `total` al vender
  (§6.2 del README) y **descuenta stock** dentro de la transacción (§6.3).
- Si insertás ventas por SQL, tenés que replicar a mano `calcularPrecioVenta()` y
  descontar stock vos mismo → cualquier error deja la recaudación y el stock
  inconsistentes.

Por eso: **grupos y clientes** se pueden cargar por API (recomendado) o por SQL si
querés volumen; **las ventas SIEMPRE por la API**.

> En prod NO corre `data-dev.sql` (ese seed es solo del perfil `dev`). Por eso prod
> nace casi vacía: solo roles (Flyway `V2`) + el admin del bootstrap. Esta ingesta
> llena el resto.

---

## 1. Prerrequisitos

- [ ] Backend **levantado** en Heroku (`…/swagger-ui/index.html` carga).
- [ ] Credenciales del **admin** (las de `ADMIN_USERNAME` / `ADMIN_PASSWORD`).
- [ ] Una herramienta para pegarle a la API. Elegí una:
  - **Swagger UI** — manual, visual, ideal para pocos registros.
  - **curl / bash** — scriptable, ideal para cargar un lote (lo de este doc).
  - **`.http`** (REST Client de VS Code / IntelliJ) — repetible.

Base URL del backend (ajustá al tuyo real, con hash):
```
https://electrogen-back-00ac9ab8e2ff.herokuapp.com
```

---

## 2. Orden de ingesta (respeta dependencias)

El orden importa por las llaves foráneas (FK):

```
1. Login admin            → obtenés el JWT (token)
2. Empleados (opcional)   → vendedores que registran ventas
3. Grupos electrógenos    → el producto; OJO: con stock > 0
4. Clientes (entidades)   → anotá el id que devuelve cada uno
5. Ventas                 → usan entidadId + (grupoCodigo o combustible/potencia)
```

Una venta necesita: un **cliente** que exista (`entidadId`) y un **grupo** con
**stock disponible**. Por eso grupos y clientes van antes.

---

## 3. Contrato real de los endpoints

| # | Método | Ruta | Rol | Body |
|---|--------|------|-----|------|
| Login | `POST` | `/api/v1/auth/login` | 🔓 | `{username, password}` → devuelve `token` |
| Empleado | `POST` | `/api/v1/usuarios` | ADMIN | `{username, password(≥8), email, rol}` |
| Grupo | `POST` | `/api/v1/grupos-electrogenos` | ADMIN | ver §4.3 |
| Stock | `PATCH` | `/api/v1/grupos-electrogenos/{id}/stock` | ADMIN | ajuste de stock |
| Cliente | `POST` | `/api/v1/clientes` | ADMIN/EMPLEADO | `{nombre}` |
| Venta | `POST` | `/api/v1/ventas` | ADMIN/EMPLEADO | ver §4.5 |

**Valores de enums** (respetar exacto, en MAYÚSCULAS):

| Enum | Valores |
|------|---------|
| `tipoCombustible` | `NAFTA`, `GAS_NATURAL`, `GASOIL` |
| `tipoArranque` | `AUTOMATICO`, `MANUAL` |
| `materialEje` | `ACERO`, `ALEACION` |
| `tipoPago` | `CHEQUE`, `EFECTIVO` |
| `rol` (alta usuario) | `EMPLEADO`, `ADMIN` |

---

## 4. Paso a paso (con curl)

> Reemplazá `BASE`, usuario y password. En Windows usá **Git Bash** (no PowerShell)
> para que estos comandos corran tal cual.

### 4.1 Variables

```bash
BASE="https://electrogen-back-00ac9ab8e2ff.herokuapp.com"
ADMIN_USER="admin"
ADMIN_PASS="Electrogen2026"
```

### 4.2 Login → guardar el token

```bash
TOKEN=$(curl -s -X POST "$BASE/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" \
  | python -c "import sys,json; print(json.load(sys.stdin)['token'])")

echo "Token: $TOKEN"
```

A partir de acá, todo va con el header `Authorization: Bearer $TOKEN`.

### 4.3 Crear grupos electrógenos (con stock)

Campos: `codigo` (único), `vidaUtil`, `tipoCombustible`, `tipoArranque`, `pMin`,
`pMax`, `insonorizado`, `capo`, `stock`, y si es móvil: `esMovil=true`,
`cantidadRuedas`, `materialEje`.

```bash
# Grupo FIJO
curl -s -X POST "$BASE/api/v1/grupos-electrogenos" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
    "codigo": "GE-FIJ-001",
    "vidaUtil": 10,
    "tipoCombustible": "GASOIL",
    "tipoArranque": "AUTOMATICO",
    "pMin": 10, "pMax": 50,
    "insonorizado": true, "capo": true,
    "stock": 15,
    "esMovil": false
  }'

# Grupo MÓVIL
curl -s -X POST "$BASE/api/v1/grupos-electrogenos" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
    "codigo": "GE-MOV-001",
    "vidaUtil": 8,
    "tipoCombustible": "NAFTA",
    "tipoArranque": "MANUAL",
    "pMin": 5, "pMax": 20,
    "insonorizado": false, "capo": false,
    "stock": 10,
    "esMovil": true,
    "cantidadRuedas": 4,
    "materialEje": "ACERO"
  }'
```

> ⚠️ **Stock > 0 es clave**: una venta contra un grupo sin stock devuelve `409`.

### 4.4 Crear clientes (entidades) — anotar el `id`

```bash
curl -s -X POST "$BASE/api/v1/clientes" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre": "Constructora del Sur S.A."}'
# → la respuesta trae el "id" (ej. 1). Anotalo: lo necesitás como entidadId.

curl -s -X POST "$BASE/api/v1/clientes" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre": "Minera Andes SRL"}'
```

Listar para ver los ids asignados:
```bash
curl -s "$BASE/api/v1/clientes" -H "Authorization: Bearer $TOKEN"
```

### 4.5 Crear ventas (el back calcula precio + descuenta stock)

Dos modos:

- **Por grupo exacto** (recomendado para ingesta controlada): mandás `grupoCodigo`
  → vende ese grupo, valida su stock y congela su precio. Sin tasación.
- **Por tasación automática**: NO mandás `grupoCodigo` → el back elige el grupo del
  combustible pedido cuya `pMax` cubra la `potenciaRequerida` (§6.3).

Campos: `nombreSolicitante`, `tipoPago`, `cantidad`, `potenciaRequerida`,
`tipoCombustible`, `vidaUtilSolicitada`, `entidadId`, y opcional `grupoCodigo`.

```bash
# Venta por grupo exacto
curl -s -X POST "$BASE/api/v1/ventas" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
    "nombreSolicitante": "Juan Pérez",
    "tipoPago": "EFECTIVO",
    "cantidad": 2,
    "potenciaRequerida": 30,
    "tipoCombustible": "GASOIL",
    "vidaUtilSolicitada": 10,
    "entidadId": 1,
    "grupoCodigo": "GE-FIJ-001"
  }'

# Venta por tasación automática (sin grupoCodigo)
curl -s -X POST "$BASE/api/v1/ventas" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
    "nombreSolicitante": "María Gómez",
    "tipoPago": "CHEQUE",
    "cantidad": 1,
    "potenciaRequerida": 15,
    "tipoCombustible": "NAFTA",
    "vidaUtilSolicitada": 8,
    "entidadId": 2
  }'
```

### 4.6 (Opcional) Crear empleados

```bash
curl -s -X POST "$BASE/api/v1/usuarios" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
    "username": "jperez",
    "password": "password123",
    "email": "jperez@electrogenos.local",
    "rol": "EMPLEADO"
  }'
```

> Si querés que las ventas queden a nombre de un empleado, logueate como ese
> empleado (mismo `/auth/login`) y registrá las ventas con SU token.

---

## 5. Evaluación / verificación

Confirmá que la ingesta entró bien:

```bash
# Catálogo (público, no necesita token)
curl -s "$BASE/api/v1/grupos-electrogenos"

# Clientes
curl -s "$BASE/api/v1/clientes" -H "Authorization: Bearer $TOKEN"

# Ventas
curl -s "$BASE/api/v1/ventas" -H "Authorization: Bearer $TOKEN"

# Reportes (solo ADMIN) — validan que los totales congelados cuadran
curl -s "$BASE/api/v1/ventas/ingresos-totales" -H "Authorization: Bearer $TOKEN"
curl -s "$BASE/api/v1/ventas/ranking-clientes"  -H "Authorization: Bearer $TOKEN"
curl -s "$BASE/api/v1/ventas/reporte-pagos"     -H "Authorization: Bearer $TOKEN"
```

Checklist de "salió bien":

- [ ] El catálogo lista los grupos creados, con su `stock`.
- [ ] Cada venta bajó el stock del grupo correspondiente.
- [ ] `ingresos-totales` ≈ suma de `total` de las ventas.
- [ ] El front muestra el catálogo y, logueado, las ventas/reportes.

También podés mirar la base directo en **Supabase → SQL Editor**:
```sql
select count(*) from grupos_electrogenos;
select count(*) from solicitudes_compra;
select id, nombre from entidades;
```

---

## 6. Troubleshooting

| Respuesta | Qué significa | Solución |
|-----------|---------------|----------|
| `401 Unauthorized` | Token vencido (dura 1h) o ausente | Re-login (§4.2) y reusar el nuevo token |
| `403 Forbidden` | El rol no alcanza para ese endpoint | Usar token de ADMIN para grupos/usuarios/reportes |
| `400` con mapa `campo→mensaje` | Validación: falta un campo o enum inválido | Revisar payload contra §3/§4 (enums en MAYÚSCULAS) |
| `409 Conflict` (stock) | El grupo no tiene stock suficiente | Subir stock (PATCH `/{id}/stock`) o bajar `cantidad` |
| `409 Conflict` (duplicado) | `codigo` de grupo repetido | Usar un `codigo` único |
| `404` en venta | `entidadId` o `grupoCodigo` no existe | Crear el cliente/grupo primero; verificar el id |

---

## 7. Si algo sale mal: reset total

La base de prod es de demo (sin datos reales que perder). Para empezar de cero:

1. **Supabase → SQL Editor:**
   ```sql
   DROP SCHEMA public CASCADE;
   CREATE SCHEMA public;
   GRANT ALL ON SCHEMA public TO postgres;
   GRANT ALL ON SCHEMA public TO anon, authenticated, service_role;
   ```
2. **Heroku backend → Deploy Branch** (o Restart): Flyway recrea `V1..V7` limpio y el
   bootstrap recrea el admin.
3. Volver a correr esta ingesta desde §4.

> Esto borra TODO el schema `public`. Solo hacelo en demo, nunca con datos reales.

---

_Relacionado: reglas de precio/stock en `README.md` §6; despliegue en `README.md` §14._

# 🚀 Plan de Despliegue — Electrogen (Heroku)

> **Frontend Astro 6 (SSR Node) → Heroku** | **Backend Spring Boot → Heroku**
> **DB → Supabase PostgreSQL (pooler)**
> Host: Heroku con créditos (~$250). Dynos **Basic** (no duermen).
> Leer junto con `DEPLOY_NOTES.md` (correcciones críticas de DB y auth).

---

## 🗺️ Arquitectura final

```
┌─────────────────────────────────────────────────────────┐
│  Usuarios / Profesores (navegador)                       │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTPS
                        ▼
┌─────────────────────────────────────────────────────────┐
│  HEROKU — app frontend                                   │
│  https://electrogen-front.herokuapp.com                  │
│  Astro 6 SSR (@astrojs/node standalone) + React 19       │
│  Node 22 · escucha en $PORT                              │
└───────────────────────┬─────────────────────────────────┘
                        │ fetch + Authorization: Bearer <JWT>
                        │ HTTPS + CORS ✅
                        ▼
┌─────────────────────────────────────────────────────────┐
│  HEROKU — app backend                                    │
│  https://electrogen-back.herokuapp.com                   │
│  Spring Boot — perfil prod · escucha en $PORT            │
│  JWT · Roles ADMIN/EMPLEADO/USER                         │
└───────────────────────┬─────────────────────────────────┘
                        │ JDBC (pooler)
                        ▼
┌─────────────────────────────────────────────────────────┐
│  SUPABASE PostgreSQL                                      │
│  aws-0-<region>.pooler.supabase.com:6543                 │
│  Flyway (V1..V6) + ddl-auto=validate                     │
└─────────────────────────────────────────────────────────┘
```

> **Dos apps Heroku separadas** (back y front). Una sola cuenta, mismos créditos.
> Cada app = su propio dyno Basic. CORS conecta front → back.

---

## ✅ Checklist rápido (TL;DR)

- [ ] **Frontend (local)**: Procfile + `start` script + mantener adapter Node (ver Parte 1)
- [ ] **Backend (local)**: puerto `${PORT:8082}` + Procfile + `system.properties` (Java)
- [ ] **Supabase**: crear proyecto, copiar datos del pooler (`DB_*`)
- [ ] **Git**: `commit + push` en ambos repos
- [ ] **Heroku app backend**: crear, conectar repo, env vars (`DB_*`, `JWT_SECRET_PROD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `SPRING_PROFILES_ACTIVE=prod`), deploy → URL
- [ ] **Heroku app frontend**: crear, conectar repo, `PUBLIC_API_BASE_URL` + `HOST=0.0.0.0` (config vars ANTES del build), deploy → URL
- [ ] **Backend**: setear `ALLOWED_ORIGINS` con la URL del front → redeploy
- [ ] **Prueba final**: login, catálogo, venta, reporte — desde otro dispositivo

---

## PARTE 1 — FRONTEND: todo lo que hay que hacer ACÁ antes de Heroku

> Estos cambios van **antes** de hacer push. El front es Astro SSR con
> `@astrojs/node` standalone — Heroku corre Node y ejecuta el server JS.
> **NO se cambia el adapter** (eso era solo para Vercel). Lo que falta es
> decirle a Heroku cómo arrancar el server y que escuche en `$PORT`.

### 1.1 Confirmar el adapter (NO tocar) `[1 min]`

`astro.config.mjs` ya está correcto para Heroku:

```js
output: "server",
adapter: node({ mode: "standalone" }),
```

El modo `standalone` genera `dist/server/entry.mjs`, un server Node autónomo que
lee las env `HOST` y `PORT`. Heroku inyecta `PORT`. **No instalar `@astrojs/vercel`.**

### 1.2 Agregar `start` script al `package.json` `[2 min]`

Heroku (buildpack Node) corre `npm run build` automáticamente si existe el script
`build` (ya existe), y luego `npm start`. Hay que agregar `start`:

```json
"scripts": {
  "dev": "astro dev",
  "build": "astro build",
  "preview": "astro preview",
  "start": "node ./dist/server/entry.mjs",
  "astro": "astro"
}
```

### 1.3 Crear el `Procfile` en la raíz del front `[1 min]`

Archivo `Procfile` (sin extensión, raíz del repo frontend):

```
web: node ./dist/server/entry.mjs
```

> Redundante con `npm start`, pero explícito = menos sorpresas. Heroku usa este
> proceso `web` para enrutar el tráfico HTTP al dyno.

### 1.4 Binding a `0.0.0.0` `[1 min]`

El server standalone por defecto escucha en `localhost`. En Heroku hay que
bindear a `0.0.0.0` o el router no llega al dyno. Se resuelve con una **config
var** en Heroku (Parte 3, no en código):

```
HOST = 0.0.0.0
```

> No hardcodear. Heroku ya inyecta `PORT`; solo falta `HOST`.

### 1.5 `PUBLIC_API_BASE_URL` se inlinea en BUILD `[recordatorio]`

⚠️ Astro mete las vars `PUBLIC_*` en el bundle **al compilar**, no en runtime.
Heroku expone las config vars durante el build, así que hay que setear
`PUBLIC_API_BASE_URL` **antes del primer deploy** (Parte 3). Si después cambia la
URL del backend, **redeploy obligatorio** del front (no basta editar la var).

> Orden correcto: tener la URL del backend Heroku ANTES de buildear el front.

### 1.6 Verificar build local `[2 min]`

```bash
npm run build
node ./dist/server/entry.mjs   # debe levantar; Ctrl+C para cortar
```

Si el build pasa y el server arranca, el front está listo para Heroku.

### 1.7 Auth ya verificada ✅

El front usa `Authorization: Bearer` (no cookie cross-site) — confirmado en
`src/lib/api/http.ts` (`getAuthHeaders()`). No hay nada que tocar acá. La cookie
`jwt_token` solo la usa el SSR de Astro (same-site, OK).

### 1.8 Commit `[1 min]`

```bash
git add package.json Procfile
git commit -m "chore: heroku frontend deploy config (start script + Procfile)"
git push
```

---

## PARTE 2 — BACKEND: cambios locales antes de Heroku

> Resumen frontend-side. El detalle fino de DB/vars está en `DEPLOY_NOTES.md`.

### 2.1 Puerto dinámico `[2 min]`

`application.properties` hoy tiene `server.port=8082` **hardcodeado** (línea 5).
Heroku inyecta `$PORT` → cambiar a:

```properties
server.port=${PORT:8082}
```

### 2.2 Procfile del backend `[1 min]`

`Procfile` en la raíz del repo backend:

```
web: java -Dserver.port=$PORT -jar target/*.jar
```

### 2.3 Versión de Java `[1 min]`

`system.properties` en la raíz del backend (ajustar a la versión real del proyecto):

```
java.runtime.version=21
```

### 2.4 Perfil prod + Postgres + Flyway

El backend real usa **perfil `prod` + Flyway + `ddl-auto=validate`** sobre
Postgres. **Nada de H2. Nada de `ddl-auto=update`.** (Ver `DEPLOY_NOTES.md`.)
Confirmar dependencia `org.postgresql:postgresql` en el `pom.xml`. ✅ Verificado.
Flyway corre `V1..V6`. Java 21 (`<java.version>21</java.version>` en `pom.xml`).

### 2.5 Admin bootstrap = env vars OBLIGATORIAS `[recordatorio]`

⚠️ `application-prod.properties` lee `ADMIN_USERNAME` y `ADMIN_PASSWORD`
**sin default** → si faltan, el backend **crashea al arrancar** (fail-fast). No
hay `admin`/`admin123` en prod: esos son defaults solo de dev. El primer admin
se crea con lo que pongas en estas vars. `ADMIN_EMAIL` es opcional (default
`admin@electrogenos.local`).

| Var              | Obligatoria | Nota                                |
| ---------------- | ----------- | ----------------------------------- |
| `ADMIN_USERNAME` | ✅          | usuario del primer admin            |
| `ADMIN_PASSWORD` | ✅          | contraseña del primer admin         |
| `ADMIN_EMAIL`    | ❌          | default `admin@electrogenos.local`  |

> Anotá usuario/contraseña que elijas: son los que vas a usar para loguear en la
> expo (Parte 6 y "Día de la expo").

---

## PARTE 3 — Heroku: crear las dos apps

> Vía dashboard de Heroku + GitHub deploys (sin CLI obligatorio). Cada app se
> conecta a su repo y deploya con un click. Usar dynos **Basic** (no duermen).

### 3.1 Crear cuenta + reclamar créditos `[3 min]`

1. **[heroku.com](https://heroku.com)** → Sign Up / Login.
2. GitHub Student Pack → Heroku: reclamar créditos en la página de billing.
3. Agregar método de pago (los créditos cubren el gasto; Basic dyno ~$7/mes).

### 3.2 App backend `[8 min]`

1. Dashboard → **New → Create new app** → nombre `electrogen-back`.
2. Tab **Deploy** → Deployment method = **GitHub** → conectar el repo del backend.
3. Tab **Settings → Config Vars**, agregar (de `DEPLOY_NOTES.md`):

| Var                      | Valor                                 |
| ------------------------ | ------------------------------------- |
| `SPRING_PROFILES_ACTIVE` | `prod`                                |
| `DB_HOST`                | `aws-0-<region>.pooler.supabase.com`  |
| `DB_PORT`                | `6543`                                |
| `DB_NAME`                | `postgres`                            |
| `DB_USER`                | `postgres.<ref>` (usuario del pooler) |
| `DB_PASSWORD`            | tu password Supabase                  |
| `JWT_SECRET_PROD`        | secreto largo random                  |
| `ADMIN_USERNAME`         | usuario del primer admin              |
| `ADMIN_PASSWORD`         | contraseña del primer admin           |
| `ADMIN_EMAIL`            | _(opcional — default local)_          |
| `ALLOWED_ORIGINS`        | _(vacío por ahora — Parte 4)_         |

4. Tab **Resources** → cambiar dyno a **Basic**.
5. Tab **Deploy** → **Deploy Branch**. Compila Maven (~3-8 min).
6. URL: `https://electrogen-back.herokuapp.com`. Probar
   `…/swagger-ui/index.html`.

### 3.3 App frontend `[6 min]`

1. Dashboard → **New → Create new app** → nombre `electrogen-front`.
2. Tab **Deploy** → GitHub → conectar el repo del frontend.
3. Tab **Settings → Config Vars** (⚠️ ANTES de deployar — se inlinean en build):

| Var                     | Valor                                             |
| ----------------------- | ------------------------------------------------- |
| `PUBLIC_API_BASE_URL`   | `https://electrogen-back.herokuapp.com`           |
| `HOST`                  | `0.0.0.0`                                         |
| `NPM_CONFIG_PRODUCTION` | `false` _(instala devDeps necesarias para build)_ |

4. Tab **Resources** → dyno **Basic**.
5. Tab **Deploy** → **Deploy Branch**. Corre `npm install` + `npm run build` +
   arranca con el Procfile (~2-4 min).
6. URL: `https://electrogen-front.herokuapp.com`.

---

## PARTE 4 — Conectar back ↔ front (CORS) `[3 min]`

1. App backend → **Settings → Config Vars** → editar `ALLOWED_ORIGINS`:
   ```
   https://electrogen-front.herokuapp.com
   ```
   (sin barra final; varias separadas por coma sin espacios)
2. Cambiar una config var dispara **redeploy automático** del backend (~2 min).

> El browser bloquea requests cross-domain sin CORS. El backend lee
> `ALLOWED_ORIGINS` del entorno y habilita esos orígenes.

---

## PARTE 5 — Base de datos (Supabase pooler)

> **H2 NO sirve** (Flyway + `validate` necesitan las tablas reales).
> **`ddl-auto=update` NO** (rompe el schema que maneja Flyway). Detalle completo
> en `DEPLOY_NOTES.md`.

### 5.1 Crear proyecto Supabase `[5 min]`

1. **[supabase.com](https://supabase.com)** → Sign Up con GitHub → **New Project**.
2. Región: **South America (São Paulo)**. Guardar la password.

### 5.2 Datos de conexión (POOLER, no directo) `[2 min]`

Project Settings → Database → **Connection pooling** (Transaction mode):

```
host:  aws-0-<region>.pooler.supabase.com
port:  6543
user:  postgres.<project-ref>
db:    postgres
```

> El host directo `db.xxxx.supabase.co:5432` ya casi no es IPv4-reachable.
> **Usar siempre el pooler** en las vars `DB_*` del backend.

### 5.3 Flyway crea el schema

Con perfil `prod`, Flyway corre `V1..V6` y crea las tablas; Hibernate solo
`validate`. Tras el primer arranque, cargar datos demo desde el panel admin.

---

## PARTE 6 — Verificación final `[10 min]`

Desde un dispositivo **distinto** (celular, compu de un compañero):

**Landing / catálogo público:**

- [ ] `https://electrogen-front.herokuapp.com` carga
- [ ] El catálogo se muestra (llama al backend)
- [ ] Filtros de combustible funcionan

**Auth + ADMIN:**

- [ ] Login con `ADMIN_USERNAME` / `ADMIN_PASSWORD` (los que seteaste en config vars)
- [ ] Sidebar muestra opciones ADMIN, dashboard con KPIs
- [ ] Crear grupo, editar stock, registrar venta, ver historial, ver reportes

**Flujo EMPLEADO:**

- [ ] Crear empleado, login como empleado
- [ ] Registrar venta, ve solo las propias
- [ ] No accede a reportes ni dashboard

**Performance:**

- [ ] Respuestas < 2s, funciona en la WiFi de la universidad

---

## 🎯 Día de la expo

### 30 min antes

- [ ] Abrir el front, esperar que cargue
- [ ] Abrir `…/swagger-ui/index.html` del backend (confirmar online)
- [ ] Login ADMIN (`ADMIN_USERNAME`/`ADMIN_PASSWORD`), verificar datos demo (recargar si hace falta)
- [ ] URL lista para compartir (chat / QR)

### Durante

- [ ] Compartir URL del front por WhatsApp/email del grupo
- [ ] Mostrar landing pública primero (sin login), luego flujo admin
- [ ] Varios pueden loguearse con `admin` — sesiones independientes (JWT en
      cookie local de cada navegador)

### URLs para compartir

```
Frontend:  https://electrogen-front.herokuapp.com
Admin:     usuario/contraseña = los que seteaste en ADMIN_USERNAME / ADMIN_PASSWORD
Swagger:   https://electrogen-back.herokuapp.com/swagger-ui/index.html
```

---

## 🛠️ Problemas comunes

### ❌ Front buildea pero falla con "Cannot find module 'astro'"

`NPM_CONFIG_PRODUCTION=false` no estaba seteado → Heroku no instaló devDeps.
Agregar la config var y redeploy.

### ❌ Front deploya pero el dyno crashea ("R10 Boot timeout")

El server no bindeó a `0.0.0.0:$PORT`. Verificar config var `HOST=0.0.0.0` y que
el Procfile use `node ./dist/server/entry.mjs`.

### ❌ Las llamadas a la API fallan (CORS error en consola)

1. `ALLOWED_ORIGINS` en el backend = URL exacta del front, **sin barra final**.
2. Esperar el redeploy del backend (~2 min).
3. Refrescar con Ctrl+Shift+R.

### ❌ Backend no arranca (Flyway / validate falla)

Vars `DB_*` apuntan al directo en vez del **pooler**, o `SPRING_PROFILES_ACTIVE`
no es `prod`, o falta `JWT_SECRET_PROD`. Revisar `DEPLOY_NOTES.md`.

### ❌ Backend crashea al arrancar pidiendo `ADMIN_USERNAME` / `ADMIN_PASSWORD`

El bootstrap del primer admin no tiene defaults → fail-fast. Setear ambas config
vars en el backend y redeploy. (Ver Parte 2.5.)

### ❌ Cambié `PUBLIC_API_BASE_URL` y el front sigue llamando a la vieja

Se inlinea en build → **redeploy** del front (Deploy Branch de nuevo).

### ❌ El dyno duerme / cold start

Usar dyno **Basic** (no Eco). Basic no duerme. Confirmar en Resources.

---

## 📦 Resumen de config vars

### Heroku — app backend

| Var                                                           | Valor           | Cuándo   |
| ------------------------------------------------------------- | --------------- | -------- |
| `SPRING_PROFILES_ACTIVE`                                      | `prod`          | Al crear |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | pooler Supabase | Al crear |
| `JWT_SECRET_PROD`                                             | secreto random  | Al crear |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD`                           | admin inicial   | Al crear |
| `ADMIN_EMAIL`                                                 | opcional        | Al crear |
| `ALLOWED_ORIGINS`                                             | URL del front   | Parte 4  |

### Heroku — app frontend

| Var                     | Valor           | Cuándo                 |
| ----------------------- | --------------- | ---------------------- |
| `PUBLIC_API_BASE_URL`   | URL del backend | ANTES del primer build |
| `HOST`                  | `0.0.0.0`       | Al crear               |
| `NPM_CONFIG_PRODUCTION` | `false`         | Al crear               |

---

_Proyecto: Electrogen — gestión de grupos electrógenos_
_Stack: Astro 6 + React 19 + Spring Boot + JWT + Postgres_
_Despliegue: Heroku (front + back) + Supabase (DB)_

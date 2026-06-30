# 🚀 Plan de Despliegue — Electrogen (Vercel + Railway)

> **Frontend Astro 6 → Vercel** | **Backend Spring Boot → Railway**  
> Tiempo estimado total: **~50 minutos**

---

## 🗺️ Arquitectura final

```
┌─────────────────────────────────────────────────────────┐
│  Usuarios / Profesores (navegador)                       │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTPS
                        ▼
┌─────────────────────────────────────────────────────────┐
│  VERCEL                                                   │
│  https://electrogen.vercel.app                           │
│  Astro 6 SSR + React 19 islands + Tailwind v4            │
│  Node 22 runtime                                         │
└───────────────────────┬─────────────────────────────────┘
                        │ fetch + Authorization: Bearer <JWT>
                        │ HTTPS + CORS ✅
                        ▼
┌─────────────────────────────────────────────────────────┐
│  RAILWAY                                                  │
│  https://electrogen.up.railway.app                       │
│  Spring Boot — puerto dinámico ($PORT)                   │
│  JWT · Roles ADMIN/EMPLEADO/USER                         │
│  Base de datos (H2 in-memory o Supabase PG)              │
└─────────────────────────────────────────────────────────┘
```

---

## ✅ Checklist rápido (TL;DR)

- [ ] **Backend**: cambiar puerto a `${PORT:8082}` en `application.properties`
- [ ] **Frontend**: instalar `@astrojs/vercel`, desinstalar `@astrojs/node`, editar `astro.config.mjs`
- [ ] **Git**: hacer `commit + push` en ambos repos (o el mismo si es monorepo)
- [ ] **Railway**: crear proyecto desde GitHub → agregar env vars → obtener URL
- [ ] **Vercel**: importar proyecto → agregar `PUBLIC_API_BASE_URL` → obtener URL
- [ ] **Railway**: agregar URL de Vercel a `ALLOWED_ORIGINS`
- [ ] **Prueba final**: login, catálogo, venta, reporte — todo desde celular con WiFi

---

## PARTE 1 — Cambios en el código

> ⚠️ Estos cambios van **antes** de hacer push a GitHub. Son 3 archivos.

### 1.1 Backend — Puerto dinámico `[5 min]`

Railway inyecta la variable de entorno `$PORT` automáticamente. Si el backend
escucha solo en 8082 hardcodeado, Railway no puede enrutar el tráfico y el
deploy falla silenciosamente.

**Archivo:** `src/main/resources/application.properties`

Busca esta línea:
```properties
server.port=8082
```

Reemplázala por:
```properties
server.port=${PORT:8082}
```

> Esto significa: "usa `$PORT` si existe, sino usa 8082". En local sigue
> funcionando en 8082. En Railway usa el puerto que Railway asigne.

**Commit sugerido:**
```bash
git add src/main/resources/application.properties
git commit -m "feat: dynamic port for Railway deployment"
git push
```

---

### 1.2 Frontend — Cambiar adapter de Astro `[15 min]`

Astro actualmente usa `@astrojs/node` (standalone server Node). Vercel necesita
su propio adapter para manejar el SSR en su infraestructura serverless.

**En tu terminal, dentro del proyecto frontend:**

```bash
# 1. Instalar el adapter de Vercel
npm install @astrojs/vercel

# 2. Desinstalar el adapter de Node
npm uninstall @astrojs/node
```

**Archivo:** `astro.config.mjs`

Antes (actual):
```js
import { defineConfig } from 'astro/config';
import react from '@astrojs/react';
import node from '@astrojs/node';         // ← quitar esto
import tailwind from '@tailwindcss/vite';

export default defineConfig({
  output: 'server',
  adapter: node({ mode: 'standalone' }),   // ← cambiar esto
  integrations: [react()],
  vite: {
    plugins: [tailwind()],
  },
});
```

Después (para Vercel):
```js
import { defineConfig } from 'astro/config';
import react from '@astrojs/react';
import vercel from '@astrojs/vercel';      // ← nuevo
import tailwind from '@tailwindcss/vite';

export default defineConfig({
  output: 'server',
  adapter: vercel(),                        // ← nuevo
  integrations: [react()],
  vite: {
    plugins: [tailwind()],
  },
});
```

> El `output: 'server'` se mantiene igual. Solo cambia la línea del adapter.

**Verificar que el build local funciona:**
```bash
npm run build
```

Si el build pasa sin errores, vas bien. Si hay error, es casi siempre por una
importación de `@astrojs/node` que quedó en algún archivo de config. Buscá:
```bash
grep -r "@astrojs/node" src/
```

**Commit sugerido:**
```bash
git add astro.config.mjs package.json package-lock.json
git commit -m "feat: switch to Vercel adapter for deployment"
git push
```

---

## PARTE 2 — Railway (Backend Spring Boot)

### 2.1 Crear cuenta en Railway `[3 min]`

1. Ir a **[railway.app](https://railway.app)**
2. Click en **"Login"** → elegir **"Login with GitHub"**
3. Autorizar Railway en GitHub

> 💡 **GitHub Student Pack → créditos Railway**  
> Después de crear la cuenta, ir a **[railway.app/account/billing](https://railway.app/account/billing)**  
> → Click en **"GitHub Student Developer Pack"** → **"Claim"**  
> Esto te da **$5/mes de créditos** — suficiente para la expo y varios días más.

---

### 2.2 Crear proyecto desde GitHub `[5 min]`

1. En el dashboard de Railway → click **"+ New Project"**
2. Elegir **"Deploy from GitHub repo"**
3. Seleccionar tu repo del backend (p.ej. `electrogen-backend`)
4. Railway detecta automáticamente que es un proyecto **Maven/Spring Boot**  
   (busca `pom.xml` en la raíz)
5. Click **"Deploy Now"**

> ⚠️ **Importante:** Si tu `pom.xml` **no está en la raíz del repo** (está en
> un subdirectorio), en la pantalla de configuración del proyecto hay que indicar
> el **Root Directory**. Ejemplo: si el backend está en `/backend/`, ponés `backend`
> en ese campo.

Railway va a:
1. Clonar el repo
2. Ejecutar `./mvnw clean package -DskipTests` (o `mvnw.cmd` en Windows)
3. Correr `java -jar target/*.jar`
4. Exponer el puerto configurado en `$PORT`

El primer deploy tarda **3–8 minutos** por la compilación de Maven.

> ⚠️ **Si falla porque `mvnw` no tiene permisos de ejecución:**  
> En tu repo local ejecutar:
> ```bash
> chmod +x mvnw
> git add mvnw
> git commit -m "fix: make mvnw executable"
> git push
> ```
> Railway hará re-deploy automáticamente.

---

### 2.3 Variables de entorno en Railway `[3 min]`

En tu proyecto Railway → click en el servicio → tab **"Variables"**:

| Variable | Valor | Descripción |
|---|---|---|
| `ALLOWED_ORIGINS` | *(lo completás en Parte 4)* | URL de tu frontend en Vercel |
| `SPRING_PROFILES_ACTIVE` | `prod` | Si tenés un `application-prod.properties` |

> Por ahora podés dejar `ALLOWED_ORIGINS` vacío y volver acá después de tener
> la URL de Vercel. El backend va a funcionar pero el browser va a bloquear las
> requests del frontend por CORS hasta que lo configures.

---

### 2.4 Obtener URL del backend `[1 min]`

1. En tu proyecto Railway → tab **"Settings"** del servicio
2. Sección **"Networking"** → click en **"Generate Domain"**
3. Te va a aparecer una URL tipo `electrogen-backend.up.railway.app`

**Guardá esta URL** — la necesitás para el paso de Vercel.

> La URL completa del backend sería:  
> `https://electrogen-backend.up.railway.app`  
> Y tu API en:  
> `https://electrogen-backend.up.railway.app/api/v1`  
> Swagger en:  
> `https://electrogen-backend.up.railway.app/swagger-ui/index.html`

---

## PARTE 3 — Vercel (Frontend Astro)

### 3.1 Crear cuenta en Vercel `[2 min]`

1. Ir a **[vercel.com](https://vercel.com)**
2. Click en **"Sign Up"** → elegir **"Continue with GitHub"**
3. Autorizar Vercel en GitHub

---

### 3.2 Importar proyecto `[3 min]`

1. En el dashboard de Vercel → click **"Add New..."** → **"Project"**
2. En "Import Git Repository", seleccionar tu repo del frontend  
   (p.ej. `electrogen-front`)
3. Vercel detecta automáticamente que es un proyecto **Astro**

> ⚠️ **Importante:** Si tu `astro.config.mjs` **no está en la raíz**, configurar
> el **Root Directory** antes de continuar. Click en "Edit" y poner el path
> relativo (ej. `frontend`).

---

### 3.3 Variables de entorno en Vercel `[2 min]`

**Antes de hacer click en "Deploy"**, en la pantalla de configuración del
proyecto, abrir la sección **"Environment Variables"** y agregar:

| Variable | Valor |
|---|---|
| `PUBLIC_API_BASE_URL` | `https://TU-APP.up.railway.app` |

> Reemplazar `TU-APP` con el nombre real de tu proyecto Railway.  
> Ejemplo: `https://electrogen-backend.up.railway.app`

Seleccionar los ambientes donde aplica: **Production**, **Preview**, y **Development**.

Después click **"Deploy"**.

El primer deploy tarda **2–4 minutos**.

---

### 3.4 URL del frontend `[1 min]`

Cuando termine el deploy, Vercel te da la URL:  
`https://electrogen-front.vercel.app` (o similar)

**Guardá esta URL** — la necesitás para el paso siguiente.

> En **Settings → Domains** podés agregar un dominio custom con tu `.me` de
> GitHub Student Pack si querés algo como `electrogen.me`. Opcional para mañana.

---

## PARTE 4 — Conectar backend ↔ frontend (CORS) `[3 min]`

Ahora tenés las dos URLs. Hay que decirle al backend que acepte requests de
la URL de Vercel.

### Actualizar ALLOWED_ORIGINS en Railway

1. Ir a tu proyecto Railway → servicio → tab **"Variables"**
2. Editar la variable `ALLOWED_ORIGINS` con la URL de Vercel:

```
https://electrogen-front.vercel.app
```

> Si querés permitir también el local para seguir desarrollando:
> ```
> https://electrogen-front.vercel.app,http://localhost:4321
> ```
> (separados por coma, sin espacios)

3. Railway hace **redeploy automático** al guardar las variables

El backend tarda ~2 minutos en volver a estar online.

---

### ¿Por qué es necesario este paso?

El browser bloquea requests HTTP entre dominios distintos (CORS). Sin esto,
cuando el frontend en `vercel.app` intenta llamar al backend en `railway.app`,
el browser rechaza la request antes de que llegue al servidor.

Tu backend ya tiene el código para leer `ALLOWED_ORIGINS` del entorno y
configurar CORS dinámicamente. Esta variable activa ese mecanismo.

---

## PARTE 5 — Base de datos: Railway Postgres `[10 min]`

> ⚠️ **No uses H2 in-memory.** Este backend corre el perfil `prod` con
> `ddl-auto=validate` + Flyway (migraciones `V1..V6`). H2 vacío no tiene esas
> tablas → `validate` falla → **la app no arranca**. Necesitás Postgres real.

Railway Postgres vive en el mismo proyecto que el backend, lo pagás con los
créditos del Student Pack y Railway inyecta las credenciales por referencia
(no copiás strings a mano).

### 5.1 Crear la base Postgres `[2 min]`

1. En tu proyecto Railway → click **"+ New"** (o **"+ Create"**)
2. Elegir **"Database"** → **"Add PostgreSQL"**
3. Railway crea un servicio `Postgres` al lado de tu backend

Esto genera automáticamente las variables internas `PGHOST`, `PGPORT`,
`PGDATABASE`, `PGUSER`, `PGPASSWORD` en el servicio Postgres.

### 5.2 Conectar el backend a la base `[3 min]`

En el servicio del **backend** (no el de Postgres) → tab **"Variables"**.
Tu app lee `DB_HOST/DB_PORT/...`, así que mapeás las de Railway por
**referencia** (sintaxis `${{Postgres.VAR}}`):

| Variable (backend) | Valor |
|---|---|
| `DB_HOST` | `${{Postgres.PGHOST}}` |
| `DB_PORT` | `${{Postgres.PGPORT}}` |
| `DB_NAME` | `${{Postgres.PGDATABASE}}` |
| `DB_USER` | `${{Postgres.PGUSER}}` |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |

> `PGHOST` apunta al host **interno** de Railway (`postgres.railway.internal`).
> El tráfico backend↔base es por red privada — más rápido y no consume egress.

### 5.3 Resto de variables obligatorias `[3 min]`

El perfil `prod` hace **fail-fast**: si falta `JWT_SECRET_PROD`,
`ADMIN_USERNAME` o `ADMIN_PASSWORD`, el backend **no arranca** (a propósito,
para no quedar con credenciales por default en producción). Agregá:

| Variable | Valor | Notas |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Activa `application-prod.properties` |
| `JWT_SECRET_PROD` | *(string largo y random)* | Mín. 32 chars para HS256. Generá uno, no lo inventes corto |
| `ADMIN_USERNAME` | `admin` | Usuario del primer admin (bootstrap) |
| `ADMIN_PASSWORD` | *(tu contraseña)* | Con esto entrás en la expo. **No la compartas si es real** |
| `ADMIN_EMAIL` | `admin@electrogenos.local` | Opcional, tiene default |
| `ALLOWED_ORIGINS` | *(URL de Vercel — Parte 4)* | Sin barra final |

> Generar un `JWT_SECRET_PROD` rápido (cualquiera sirve):
> ```bash
> openssl rand -base64 48
> ```

### 5.4 Redeploy `[2 min]`

Al guardar las variables Railway hace **redeploy automático**. En el arranque:
1. Flyway corre `V1..V6` → crea el esquema en Postgres
2. Hibernate **valida** que las entidades matcheen (no toca el esquema)
3. El bootstrap crea el admin con `ADMIN_USERNAME` / `ADMIN_PASSWORD`

> Mirá los **logs** del deploy. Si ves `Flyway ... migrating schema` y después
> `Started ...Application`, está sano. Si ves `Schema-validation` o
> `relation does not exist`, la base no migró — revisá que Flyway esté activo.

> ✅ Los datos **persisten** entre reinicios. No hay que recargar nada antes
> de la expo. Cargá los grupos/ventas una vez y quedan.

---

### 5.5 Alternativa: si Railway no está disponible (Render / Fly.io)

Casi nada del plan cambia. `$PORT`, perfil `prod`, `validate` + Flyway, build
`mvnw` y **todas las vars** (`JWT_SECRET_PROD`, `ADMIN_USERNAME`,
`ADMIN_PASSWORD`, `ALLOWED_ORIGINS`, `SPRING_PROFILES_ACTIVE`) son iguales.

**Lo único que cambia: cómo cableás la base.** Estas plataformas dan un solo
`DATABASE_URL` en formato URI (no las vars `PGHOST/PGPORT/...` por referencia
como Railway):
```
postgresql://usuario:password@host:5432/basededatos
```

Dos opciones:

**Opción 1 — cargar las `DB_*` a mano** (desglosar la URI):

| Variable | De la URI sale de... |
|---|---|
| `DB_HOST` | el `host` |
| `DB_PORT` | el puerto (`5432`) |
| `DB_NAME` | la base después de la última `/` |
| `DB_USER` | el usuario antes de `:` |
| `DB_PASSWORD` | el password entre `:` y `@` |

**Opción 2 — usar `SPRING_DATASOURCE_*` directo** (más limpio, cross-platform):

| Variable | Valor |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://host:5432/basededatos` |
| `SPRING_DATASOURCE_USERNAME` | `usuario` |
| `SPRING_DATASOURCE_PASSWORD` | `password` |

> Spring toma estas y **pisan** el datasource del perfil prod (las `DB_*`
> quedan sin usar). Mismo `validate` + Flyway, **sin tocar código**. Ojo:
> `SPRING_DATASOURCE_URL` lleva el prefijo `jdbc:` y la URI de la plataforma
> normalmente **no** lo trae — agregalo.

---

## PARTE 6 — Verificación final `[10 min]`

Antes de la expo, verificar este flujo completo desde un dispositivo **diferente
al tuyo** (celular, la compu de un compañero):

### Checklist de verificación

**Landing pública:**
- [ ] `https://tu-app.vercel.app` carga correctamente
- [ ] El catálogo de grupos electrógenos se muestra (llama al backend)
- [ ] Los filtros de combustible funcionan

**Autenticación:**
- [ ] Login con `admin` / `admin123` funciona
- [ ] El sidebar muestra las opciones de ADMIN
- [ ] El dashboard muestra KPIs

**Operaciones de ADMIN:**
- [ ] Crear un grupo electrógeno nuevo
- [ ] Editar stock
- [ ] Registrar una venta
- [ ] Ver historial de ventas
- [ ] Ver reportes (ranking clientes, por empleado, por pago, ingresos)

**Flujo de EMPLEADO:**
- [ ] Crear un usuario empleado desde gestión de empleados
- [ ] Cerrar sesión y hacer login con el empleado
- [ ] Registrar una venta
- [ ] Verificar que solo ve sus propias ventas
- [ ] Verificar que no puede entrar a reportes ni dashboard

**Performance:**
- [ ] Las respuestas son rápidas (< 2 segundos)
- [ ] Funciona en la WiFi de la universidad (prueba desde celular con esa red)

---

## 🎯 Checklist del día de la exposición

### 30 minutos antes

- [ ] Abrir `https://tu-app.vercel.app` — esperar que cargue
- [ ] Abrir `https://backend.up.railway.app/swagger-ui/index.html` — confirmar
  que el backend está activo (si tarda, es cold start, esperar 1-2 min)
- [ ] Hacer login como ADMIN y verificar que los datos de prueba están ahí
- [ ] Si los datos se perdieron (reinicio de H2), volver a cargarlos ahora
- [ ] Tener la URL lista para compartir por chat o QR

### Durante la exposición

- [ ] Compartir la URL por WhatsApp/email del grupo para que todos prueben
- [ ] Mostrar primero la landing pública (no requiere login)
- [ ] Luego demostrar el flujo de admin
- [ ] Si alguien se loguea con el mismo `admin`, está bien — las sesiones son
  independientes (JWT en cookie local de cada navegador)

### URL para compartir

```
Frontend:   https://tu-app.vercel.app
Admin:      usuario: admin | contraseña: admin123
Swagger:    https://backend.up.railway.app/swagger-ui/index.html
```

---

## 🛠️ Problemas comunes y soluciones

### ❌ "Application failed to build" en Railway

**Causa más común:** `mvnw` no tiene permisos de ejecución.

```bash
# En tu repo local del backend:
chmod +x mvnw
git add mvnw
git commit -m "fix: executable mvnw"
git push
```

---

### ❌ El frontend carga pero las llamadas a la API fallan (CORS error en consola)

**Verificar en orden:**

1. En Railway → Variables → que `ALLOWED_ORIGINS` tenga la URL exacta de Vercel
2. La URL no debe tener barra final: `https://app.vercel.app` ✅ — `https://app.vercel.app/` ❌
3. Después de cambiar la variable, esperar el redeploy en Railway (~2 min)
4. Refrescar el frontend con Ctrl+Shift+R (cache limpio)

---

### ❌ "Internal Server Error" al registrar una venta

**Causa más probable:** stock insuficiente (el backend devuelve 409).  
El frontend ya maneja esto con un mensaje de error. Verificar el stock del
grupo electrógeno que estás intentando vender.

---

### ❌ El backend se reinicia y los datos H2 desaparecen

**Antes de la expo:** cargar datos de demostración mínimos:
- 3-5 grupos electrógenos (fijos y móviles)
- 2-3 ventas de historial
- 1 usuario empleado de ejemplo

> Si esto es un riesgo real, migrar a Supabase (Parte 5 Opción B) la noche
> anterior.

---

### ❌ Vercel da error de build "Cannot find module '@astrojs/node'"

El `package.json` todavía referencia `@astrojs/node` aunque lo desinstalaste.
Verificar:

```bash
# En el frontend
cat package.json | grep "astrojs"
```

Si aparece `@astrojs/node`, eliminarlo manualmente del `package.json` y hacer
`npm install` + push.

---

### ❌ El deploy de Vercel tarda más de 10 min

El primer deploy puede tardar por la instalación de dependencias. Los
siguientes van a ser mucho más rápidos (caché de node_modules).

---

### ❌ La URL de Railway es muy larga / difícil de escribir

En Railway → Settings → Networking → Custom Domain podés poner un subdominio
de tu dominio `.me` de GitHub Student Pack:
```
api.electrogen.me → tu-app.up.railway.app
```
Pero para mañana, con la URL de Railway está bien.

---

## 📦 Resumen de env vars necesarias

### Railway (backend)

| Variable | Valor | Cuándo agregarla |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Al configurar el servicio |
| `DB_HOST` | `${{Postgres.PGHOST}}` | Parte 5 (al crear Postgres) |
| `DB_PORT` | `${{Postgres.PGPORT}}` | Parte 5 |
| `DB_NAME` | `${{Postgres.PGDATABASE}}` | Parte 5 |
| `DB_USER` | `${{Postgres.PGUSER}}` | Parte 5 |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` | Parte 5 |
| `JWT_SECRET_PROD` | *(string random ≥32 chars)* | Parte 5 — **obligatoria** |
| `ADMIN_USERNAME` | `admin` | Parte 5 — **obligatoria** |
| `ADMIN_PASSWORD` | *(tu contraseña)* | Parte 5 — **obligatoria** |
| `ADMIN_EMAIL` | `admin@electrogenos.local` | Parte 5 (opcional) |
| `ALLOWED_ORIGINS` | `https://tu-front.vercel.app` | Después de obtener URL de Vercel |

### Vercel (frontend)

| Variable | Valor | Cuándo agregarla |
|---|---|---|
| `PUBLIC_API_BASE_URL` | `https://tu-back.up.railway.app` | Antes del primer deploy |

---

## ⏱️ Timeline completo

```
T+00:00  Cambiar puerto en application.properties (backend)   → 5 min
T+05:00  Cambiar adapter en astro.config.mjs (frontend)       → 10 min
T+15:00  Push de ambos repos a GitHub                         → 3 min
T+18:00  Crear cuenta Railway + importar backend              → 8 min
T+26:00  Backend compilando en Railway (esperar)              → 8 min
T+34:00  Crear cuenta Vercel + importar frontend              → 5 min
T+39:00  Frontend deploying en Vercel (esperar)               → 4 min
T+43:00  Configurar ALLOWED_ORIGINS en Railway                → 2 min
T+45:00  Redeploy backend con nuevo CORS                      → 3 min
T+48:00  Verificación end-to-end                              → 10 min
T+58:00  ✅ Listo para la expo
```

---

*Proyecto: Electrogen — Sistema de gestión de grupos electrógenos*  
*Stack: Astro 6 + React 19 + Spring Boot + JWT*  
*Despliegue: Vercel (front) + Railway (back)*

# 📝 Deploy Notes — Correcciones y decisiones pendientes

> Complemento crítico de `DEPLOY_PLAN.md`. Leer ANTES de ejecutar la Parte 5.
> El plan original es genérico y asume cosas que NO coinciden con este backend.
> Última actualización: 2026-06-28.

---

## ⛔ Correcciones críticas al `DEPLOY_PLAN.md` (Parte 5)

El backend real es **Postgres + Flyway + `ddl-auto=validate`** con perfil `prod`.
El plan asume H2 / `ddl-auto=update` / vars `SPRING_DATASOURCE_*`. Si seguís la
Parte 5 tal cual, **la app no arranca**.

### 1. H2 in-memory (Opción A) = NO sirve
`application-prod.properties` corre `ddl-auto=validate` + Flyway (V1..V6).
H2 in-memory no tiene esas tablas → `validate` falla → app no levanta.
**Olvidate de H2 para este proyecto.**

### 2. Supabase con `ddl-auto=update` (Opción B) = MAL
Prod usa `validate` + Flyway. Mezclar `update` con Flyway = caos de schema.
Flyway crea las tablas, Hibernate solo valida.
**NO setear `SPRING_JPA_HIBERNATE_DDL_AUTO=update`.**

### 3. Las env vars del plan ≠ las que la app lee
El plan dice `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`. El backend real lee:

| Var | Obligatoria | Nota |
|---|---|---|
| `DB_HOST` | ✅ | |
| `DB_PORT` | ✅ | `6543` si pooler Supabase |
| `DB_NAME` | ✅ | |
| `DB_USER` | ✅ | |
| `DB_PASSWORD` | ✅ | |
| `JWT_SECRET_PROD` | ✅ | **Sin esto = crash al arranque.** |
| `ADMIN_USERNAME` | ✅ | **Sin default → crash al arranque.** Usuario del primer admin. |
| `ADMIN_PASSWORD` | ✅ | **Sin default → crash al arranque.** Contraseña del primer admin. |
| `ADMIN_EMAIL` | ❌ | default `admin@electrogenos.local` |
| `ALLOWED_ORIGINS` | ✅ | URL del front en Heroku, sin barra final |
| `SPRING_PROFILES_ACTIVE` | ✅ | `prod` |

> El bootstrap del primer admin (`app.bootstrap.admin.*` en
> `application-prod.properties`) NO tiene defaults en username/password →
> fail-fast. `admin`/`admin123` es solo de dev. En prod el admin es el que pongas
> en `ADMIN_USERNAME`/`ADMIN_PASSWORD`.

### 4. Connection string de Supabase desactualizado
El `db.xxxx.supabase.co:5432` directo ya casi no es IPv4-reachable.
Hoy Supabase empuja el **pooler**: `aws-0-<region>.pooler.supabase.com:6543`.
El host del backend probablemente NO conecte por el directo. **Usá el pooler.**

### Camino real para la expo
Supabase (pooler) + perfil `prod` + vars `DB_*` + `JWT_SECRET_PROD` +
`ADMIN_USERNAME`/`ADMIN_PASSWORD`. Sin H2.

---

## ⚠️ Gotchas del frontend (Vercel)

1. **`PUBLIC_API_BASE_URL` se inlinea en BUILD, no en runtime.**
   Astro mete las vars `PUBLIC_*` en el bundle al compilar. Si cambiás la URL
   del backend después, editar la var en Vercel NO basta → hay que **redeploy**.
   Orden correcto: tener URL del backend ANTES de buildear el front.

2. **Verificar que el front mande `Authorization: Bearer`, no cookie cross-site.**
   Cookie `jwt_token` es `SameSite=Lax`. Front (`vercel.app`) y back
   (`railway/render/etc`) son dominios distintos → la cookie NO viaja sola
   cross-site. Funciona SOLO si el JS lee la cookie y la pone como header Bearer.
   **Pendiente: confirmar en el código del front antes de deployar.** Es el punto
   que más rompe deploys así.

---

## 🔁 Alternativas a Railway (host del backend Spring Boot)

La DB va aparte en Supabase, así que el host solo levanta el JAR con `$PORT` +
las vars `DB_*` / `JWT_SECRET_PROD` / `ALLOWED_ORIGINS`.

### 1. Render — gratis, sin Student Pack
- ✅ Flujo igual a Railway (repo GitHub → detecta build → deploy). Soporta
  `$PORT`, env vars, Docker o buildpack Java. Sin tarjeta para el free tier.
- ❌ **Duerme tras 15 min sin tráfico.** Cold start Spring ~50s. Riesgo
  mid-expo. Mitigable abriéndolo antes y dejándolo con tráfico.

### 2. Fly.io — free allowance, sin Student Pack
- ✅ No fuerza sleep (`min_machines_running=1` lo deja despierto). Mejor perf
  que Render free. Docker nativo, corre el JAR sin drama.
- ❌ Pide tarjeta (no cobra dentro del allowance). Setup por CLI (`flyctl`),
  menos point-and-click. Curva un toque más alta.

### 3. DigitalOcean App Platform — Student Pack ($200 créditos)
- ✅ **No duerme nunca** (lo más confiable para el día de la expo). $200
  alcanzan meses. Mismo flujo repo→deploy. El más robusto.
- ❌ Depende de que DO siga en el Pack (verificar). Consume créditos. No es
  "todo gratis sin créditos".

### Recomendación
- Expo donde el cold start arruina la demo → **DigitalOcean** (no duerme).
- Cero créditos y no molesta despertar el back antes → **Render**.

---

## ✅ Pendiente para mañana (TODO)

- [ ] Verificar qué sigue vigente en el **GitHub Student Pack 2026** (Railway?
      DigitalOcean? Azure? Heroku?) y límites de cada free tier.
      → Host elegido: **Heroku** (créditos ~$250), dynos Basic (no duermen).
- [x] Confirmar en el código del front que usa **`Authorization: Bearer`**
      (no cookie cross-site) para llamar a la API. ✅ Verificado: `getAuthHeaders()`
      en `src/lib/api/http.ts` lee la cookie en cliente y la manda como header
      Bearer en todos los API clients. Ningún fetch usa `credentials:'include'`.
      La cookie `jwt_token` solo la usa el SSR de Astro (same-site, OK).
- [x] Elegir host del backend. → **Heroku** (back y front como 2 apps separadas).
- [x] Reescribir el `DEPLOY_PLAN.md` con: Supabase pooler + perfil `prod` + vars
      reales (`DB_*`, `JWT_SECRET_PROD`, `ALLOWED_ORIGINS`). Plan ahora es Heroku.
- [x] Quitar del plan toda mención a H2 y a `ddl-auto=update`. Hecho.

# Pendiente — Estado para retomar

> Foto del trabajo de endurecimiento para producción. Plan completo y criterios
> de aceptación en [`PLAN-DE-ACCION.md`](PLAN-DE-ACCION.md). Veredicto y contexto
> en [`contexto/`](contexto/).

## Resumen

Son **4 fases** (cubren los 8 indispensables). Estado: **7/8 — falta solo la Fase 4.**

```
Fase 1  Seguridad de acceso     [x] I1  [x] I2  [x] I3
Fase 2  Esquema y arranque      [x] I6  [x] I7
Fase 3  Integridad de ventas    [x] I4  [x] I5
Fase 4  Robustez                [ ] I8        <-- ÚNICO PENDIENTE
```

Último commit: `feat(backend): endurecimiento prod fases 1-3`.
Suite local: **42 tests verdes** (excluyendo el de Testcontainers, ver abajo).

---

## Lo que falta para "PRODUCCIÓN READY"

### 1. Fase 4 / I8 — Handler genérico de excepciones (BLOQUEANTE)

Único indispensable abierto.

- **Problema:** `GlobalExceptionHandler` no captura `Exception` genérica → un error
  inesperado devuelve el 500 por defecto de Spring con stack trace (filtra rutas,
  librerías, a veces datos).
- **Acción:**
  - Agregar `@ExceptionHandler(Exception.class)` que responda un error genérico y
    controlado (sin stack trace).
  - Loguear el detalle completo solo en el servidor.
- **Archivo:** `src/main/java/.../exception/GlobalExceptionHandler.java`.
- **Criterio de aceptación:** test que fuerza una excepción no mapeada → respuesta
  genérica (sin stack trace en el body); el detalle aparece en logs.

Con esto cerrado, las 8 casillas quedan marcadas y la etiqueta "PRODUCCIÓN READY"
deja de ser falsa.

---

## Deuda técnica / caveats (no bloquean, pero anotados)

### A. Test de migraciones Flyway no corre en local

- `MigracionesFlywayIntegrationTest` (Testcontainers + Postgres real) **no corre
  en Windows con Docker Desktop 29**: docker-java pega al named pipe y `/info`
  devuelve `Status 400` en todas las strategies. El CLI (`docker run`/`pull`) sí
  funciona.
- **Hoy:** corre en CI (Docker Linux normal). En local se excluye:
  ```
  ./mvnw test -Dtest='!MigracionesFlywayIntegrationTest'
  ```
- **Verificación manual usada** (reproducible): levantar Postgres por CLI y correr
  la app en perfil prod → Flyway aplica V1-V4, `validate` pasa, admin sembrado.
- Si se quiere correr local: exponer el daemon por TCP (`tcp://localhost:2375`)
  en Docker Desktop. Tiene costo de seguridad (daemon sin auth).

### B. Aislamiento de tests

- Los `@SpringBootTest` corren bajo perfil `dev` (default) y comparten la mem-db
  `grupos_electrogenos_dev` (`DB_CLOSE_DELAY=-1`) → puede haber polución entre
  clases (un grupo leakeado altera el selector `OrderByPMaxDesc`).
- Mitigado en los tests nuevos con invariantes + `pMax` alto.
- **Mejora futura:** mover los `@SpringBootTest` al perfil `test` (mem-db efímera
  ya configurada) o `@Transactional` en todos.

### C. Empaquetado

- `./mvnw package` con `-DskipTests` dejó un jar sin manifest ejecutable
  (`repackage` falló). Para correr la app usar `./mvnw spring-boot:run`. Revisar
  la config de `spring-boot-maven-plugin` si se necesita el fat jar.

### D. Mejoras opcionales (producto, NO indispensables)

- Ver [`contexto/02-mejoras.md`](contexto/02-mejoras.md). Quedan fuera del plan;
  son decisión de negocio.

---

## Variables de entorno para desplegar prod

El perfil `prod` falla al arrancar (fail-fast) si faltan. Sin defaults:

| Variable          | Uso                                   |
|-------------------|---------------------------------------|
| `JWT_SECRET_PROD` | Secreto de firma JWT                  |
| `DB_PASSWORD`     | Password de PostgreSQL                |
| `ADMIN_USERNAME`  | Usuario del primer admin (bootstrap)  |
| `ADMIN_PASSWORD`  | Password del primer admin (se bcryptea)|

Opcionales con default: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`,
`ADMIN_EMAIL`, `ALLOWED_ORIGINS`.

---

## Cómo verificar lo hecho

```bash
# Suite completa (sin el de Testcontainers, que necesita Docker normal)
./mvnw test -Dtest='!MigracionesFlywayIntegrationTest'

# Migraciones contra Postgres real (manual)
docker run -d --name pg -e POSTGRES_DB=electrogenos_prod -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=secret -p 5432:5432 postgres:16-alpine
DB_HOST=localhost DB_PASSWORD=secret JWT_SECRET_PROD=<secreto-largo> \
  ADMIN_USERNAME=admin ADMIN_PASSWORD=<pass> \
  ./mvnw -Dspring-boot.run.profiles=prod spring-boot:run
# Esperar: Flyway "Successfully applied 4 migrations", app "Started", admin creado.
```

---

## Próximo paso

Arrancar **Fase 4 / I8**. Es la más corta: un `@ExceptionHandler(Exception.class)`
en `GlobalExceptionHandler` + su test. Cierra el plan.

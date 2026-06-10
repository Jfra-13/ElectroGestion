# 03 — Indispensables (no se discuten)

> Esto NO es opinión de estilo ni preferencia de producto. Son requisitos mínimos para que un
> backend que maneja **ventas, dinero y datos de clientes** pueda llamarse "listo para
> producción". Mientras estos puntos sigan abiertos, la etiqueta "PRODUCCIÓN READY" es falsa.
>
> Cada punto incluye el problema, el riesgo concreto y qué debe quedar resuelto.

---

## 🔴 SEGURIDAD

### I1. Eliminar la escalada de privilegios en el registro público
- **Problema:** `/api/v1/auth/register` es público (`SecurityConfig.java:47`) y acepta el campo
  `roles` del cliente, asignándolo sin control (`AuthController.java:79-86`,
  `RegisterRequestDTO.roles`).
- **Riesgo:** Cualquiera se auto-asigna `ROLE_ADMIN` con un solo POST. Toda la autorización
  por roles queda anulada.
- **Debe quedar:** El registro público NUNCA acepta roles del cliente; siempre asigna el rol
  mínimo (`ROLE_USER`). La creación de administradores solo la hace un admin por un endpoint
  protegido (o por seed controlado). El campo `roles` sale del DTO público.

### I2. Proteger todos los datos financieros y de clientes
- **Problema:** Todos los GET son públicos, incluidos `ingresos-totales`, `ranking-clientes`,
  `reporte-pagos`, ventas y precios (`SecurityConfig.java:49-51`).
- **Riesgo:** Exposición de facturación total, cartera de clientes y precios a cualquiera sin
  autenticar. Fuga de información confidencial del negocio.
- **Debe quedar:** Los endpoints de ventas/reportes/recaudación exigen autenticación y rol.
  Como mucho, el catálogo de productos puede ser público; la información de negocio NO.

### I3. Sin secretos por defecto en el repositorio
- **Problema:** `jwt.secret` con default committeado (`application.properties:9`) y
  `DB_PASSWORD:admin` como default de producción (`application-prod.properties:4`).
- **Riesgo:** Si una variable de entorno falta, el sistema arranca con credenciales conocidas
  y públicas. Token forjable, base accesible.
- **Debe quedar:** En producción, secreto JWT y credenciales de DB **sin valor por defecto**:
  si no están en el entorno, la app debe fallar al arrancar (fail-fast), no usar un default.

---

## 🔴 INTEGRIDAD DE DATOS / CONTABILIDAD

### I4. Congelar el precio y el total al momento de la venta
- **Problema:** `SolicitudCompra` no persiste precio unitario ni total; se recalcula en cada
  lectura desde el grupo actual (`SolicitudCompraServiceImpl.java:126`).
- **Riesgo:** Editar un grupo cambia retroactivamente la recaudación histórica. Los números
  de ventas pasadas mienten. Inaceptable para cualquier contabilidad o auditoría.
- **Debe quedar:** Al crear la venta se guardan `precioUnitario` y `total` (= unitario ×
  cantidad) como columnas inmutables. Los reportes leen esos valores guardados, no recálculos.

### I5. El stock debe moverse y validarse con la venta
- **Problema:** Crear una venta no descuenta stock ni valida disponibilidad; el campo `stock`
  solo se toca por un PATCH manual.
- **Riesgo:** Se "venden" unidades inexistentes; el inventario nunca refleja la realidad.
- **Debe quedar:** O bien la venta descuenta stock y rechaza la operación si no hay
  disponibilidad, o se elimina explícitamente el concepto de stock del dominio. Lo que no
  puede quedar es un stock que finge gestionar inventario y no lo hace.

---

## 🔴 OPERABILIDAD EN PRODUCCIÓN

### I6. Migraciones de esquema versionadas (Flyway/Liquibase)
- **Problema:** Prod usa `ddl-auto=validate` y `sql.init.mode=never`, sin herramienta de
  migración. No hay quién cree el esquema.
- **Riesgo:** En una base limpia de producción, la app no arranca: `validate` falla porque las
  tablas no existen. Literalmente no se puede desplegar.
- **Debe quedar:** Flyway (o Liquibase) con scripts versionados que creen y evolucionen el
  esquema. Es el estándar y es no-negociable para tener prod real.

### I7. Bootstrap de roles y primer administrador en producción
- **Problema:** `DataInitializer` corre solo en dev/test (`DataInitializer.java:15`). En prod
  no hay roles ni admin inicial.
- **Riesgo:** Aunque existiera el esquema, no hay `ROLE_ADMIN` ni manera de crear el primer
  usuario administrador. El sistema queda inutilizable.
- **Debe quedar:** Un mecanismo controlado de bootstrap en prod (migración seed de roles +
  creación del primer admin vía variables de entorno o comando único), sin depender del
  registro público.

---

## 🟠 ROBUSTEZ MÍNIMA

### I8. Handler genérico de excepciones
- **Problema:** `GlobalExceptionHandler` no captura `Exception` genérica.
- **Riesgo:** Cualquier error no previsto devuelve el 500 por defecto de Spring con stack
  trace, filtrando detalles internos (rutas, librerías, a veces datos).
- **Debe quedar:** Un `@ExceptionHandler(Exception.class)` que responda un error genérico y
  controlado (sin stack trace), y registre el detalle solo en logs del servidor.

---

## Checklist de "verdaderamente listo para producción"

```
[ ] I1  Registro público no asigna roles; admin solo por canal protegido
[ ] I2  Endpoints financieros/clientes detrás de auth + rol
[ ] I3  Sin defaults de JWT secret ni DB password en prod (fail-fast)
[ ] I4  Venta guarda precio unitario y total inmutables
[ ] I5  Stock se descuenta/valida en la venta (o se elimina el concepto)
[ ] I6  Migraciones Flyway/Liquibase creando el esquema
[ ] I7  Roles + primer admin sembrados en prod
[ ] I8  Handler genérico de Exception sin filtrar stack traces
```

Sobre estos ocho puntos no hay debate de gusto: son requisitos funcionales y no funcionales
de cualquier backend transaccional con datos sensibles. El resto (`02-mejoras.md`) es tu
producto y tu decisión.

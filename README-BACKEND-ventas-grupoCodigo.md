# Backend — Soporte de venta por grupo elegido (`grupoCodigo`)

**Para:** equipo Backend (Spring Boot).
**Origen:** rediseño del módulo de Ventas en el frontend
(`PLAN-FRONT-ventas-rebuild.md`).
**Tipo de cambio:** aditivo y **retrocompatible** — un campo opcional.
**Endpoint afectado:** `POST /api/v1/ventas`.

---

## 1. Por qué

Hoy `POST /ventas` recibe solo criterios (`potenciaRequerida`,
`tipoCombustible`, `vidaUtilSolicitada`) y el backend corre la **tasación**
eligiendo él mismo el grupo. Problema: varios grupos pueden cumplir los mismos
criterios, así que el backend puede asignar un grupo distinto al que el vendedor
quería, con **otro precio y otro stock**.

Caso real que rompe la experiencia:

1. El vendedor quiere vender **GE-FIJ-005** (Nafta, 2–8 kVA, stock 20).
2. El front manda potencia = 5, Nafta, vida 10.
3. Cumplen GE-FIJ-005 **y** GE-MOV-002 (Nafta, 4–15 kVA). La tasación elige el
   más barato → **GE-MOV-002**, que tiene **stock 0** → `409`.
4. El vendedor recibe "Stock insuficiente para GE-MOV-002" sin haber elegido ese
   grupo. Confusión total.

**Objetivo:** que el vendedor pueda vender un grupo **concreto** y se venda ese,
con su precio y su stock.

## 2. Qué hay que hacer (resumen)

Agregar a `POST /api/v1/ventas` un campo **opcional** `grupoCodigo`:

- **Si viene** → vender **ese** grupo exacto (sin tasación). Validar su stock y
  congelar **su** precio.
- **Si no viene** → comportamiento actual (tasación). **No cambia nada.**

Eso es todo el cambio funcional. El resto del contrato (response, otros campos,
roles, atribución de vendedor por JWT) **se mantiene igual**.

## 3. Request

### Antes (sigue siendo válido)
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

### Después (con grupo elegido)
```json
{
  "nombreSolicitante": "SAC VERCER MINERA",
  "tipoPago": "CHEQUE",
  "cantidad": 19,
  "potenciaRequerida": 5.0,
  "tipoCombustible": "NAFTA",
  "vidaUtilSolicitada": 10,
  "entidadId": 1,
  "grupoCodigo": "GE-FIJ-005"
}
```

| Campo | Tipo | Obligatorio | Regla |
| ----- | ---- | ----------- | ----- |
| `grupoCodigo` | string | ❌ opcional | si viene, debe existir un grupo con ese código |

> El front, cuando manda `grupoCodigo`, rellena `potenciaRequerida`,
> `tipoCombustible` y `vidaUtilSolicitada` con valores **derivados del propio
> grupo elegido**, de modo que son coherentes con él. El backend igualmente puede
> validarlo (ver §5, recomendado).

## 4. Comportamiento esperado

```
¿viene grupoCodigo?
├── SÍ  → buscar grupo por código
│         ├── no existe        → 404
│         ├── stock < cantidad → 409 (mensaje nombra ESE código)
│         └── ok               → vender ese grupo:
│                                 - congelar SU precio de venta calculado
│                                 - descontar SU stock (atómico)
│                                 - grabar la venta con ese grupoId/grupoCodigo
│
└── NO  → tasación actual (sin cambios)
```

Cuando `grupoCodigo` viene, **NO** correr la tasación: el grupo ya está decidido.
La operación de **verificar stock + descontar** debe ser **atómica** sobre el
grupo elegido (igual que hoy), para no vender por encima del stock en concurrencia.

## 5. Validaciones y errores

| Situación | Código | Mensaje (sugerido) |
| --------- | ------ | ------------------ |
| `grupoCodigo` no existe | `404` | `Grupo no encontrado: {grupoCodigo}` |
| Stock insuficiente del grupo elegido | `409` | `Stock insuficiente para el grupo {grupoCodigo}: disponible {stock}, solicitado {cantidad}` |
| `cantidad < 1` u otros campos inválidos | `400` | validación estándar |
| `entidadId` inexistente | `404` | igual que hoy |
| Sin rol ADMIN/EMPLEADO | `403` | igual que hoy |

**Importante:** el `409` debe nombrar **el grupo elegido por el vendedor**
(`grupoCodigo`), no otro. Ese es justo el bug que estamos eliminando.

**Recomendado (coherencia, opcional):** si `grupoCodigo` viene y los criterios no
encajan con el grupo (p. ej. `potenciaRequerida` fuera de `[pMin, pMax]` o
`tipoCombustible` distinto), responder `400`/`422` con un mensaje claro. Evita
ventas con datos incoherentes. Si se prefiere simplicidad, basta con que el grupo
elegido mande y los criterios se graben tal cual llegan.

## 6. Response (no cambia)

La respuesta `201` mantiene exactamente la forma actual
(`SolicitudCompraResponseDTO`). Cuando se usa `grupoCodigo`, `grupoId` /
`grupoCodigo` / `precioVentaUnitario` / `total` deben reflejar **el grupo
elegido**:

```json
{
  "id": 42,
  "identificador": "VTA-0042",
  "nombreSolicitante": "SAC VERCER MINERA",
  "tipoPago": "CHEQUE",
  "cantidad": 19,
  "potenciaRequerida": 5.0,
  "tipoCombustible": "NAFTA",
  "vidaUtilSolicitada": 10,
  "entidadId": 1,
  "entidadNombre": "SAC VERCER MINERA",
  "grupoId": 5,
  "grupoCodigo": "GE-FIJ-005",
  "precioVentaUnitario": 250.0,
  "total": 4750.0,
  "vendedorId": 5,
  "vendedorUsername": "vendedor1"
}
```

## 7. Compatibilidad

- `grupoCodigo` es **opcional**. Clientes/integraciones existentes que no lo
  envían siguen funcionando con tasación. **No romper** ese camino.
- El precio se **congela** en la venta (igual que hoy): editar el grupo después no
  altera ventas ya registradas.
- Atribución de vendedor por JWT: **sin cambios** (no se manda en el body).

## 8. Casos de prueba (criterios de aceptación)

1. **Venta por grupo, feliz:** `grupoCodigo=GE-FIJ-005`, stock 20, cantidad 5 →
   `201`, vende GE-FIJ-005, descuenta stock a 15, `total = 250 × 5`.
2. **El bug eliminado:** `grupoCodigo=GE-FIJ-005` aunque GE-MOV-002 también cumpla
   los criterios → vende **GE-FIJ-005** (no GE-MOV-002).
3. **Stock insuficiente del elegido:** `grupoCodigo=GE-MOV-002` (stock 0),
   cantidad 1 → `409` nombrando **GE-MOV-002**.
4. **Código inexistente:** `grupoCodigo=GE-XXX-999` → `404`.
5. **Sin `grupoCodigo`:** request actual → tasación, **igual que antes**
   (regresión: no debe cambiar).
6. **Concurrencia:** dos ventas simultáneas del mismo grupo con stock 1 → solo una
   tiene éxito; la otra `409`.

## 9. Checklist backend

- [ ] `grupoCodigo` opcional aceptado en `POST /api/v1/ventas`.
- [ ] Con `grupoCodigo`: se omite la tasación y se vende ese grupo.
- [ ] Validación de existencia (`404`) y stock (`409`) sobre el grupo elegido.
- [ ] `409` nombra el grupo elegido por el vendedor.
- [ ] Precio congelado y stock descontado del grupo elegido (atómico).
- [ ] Sin `grupoCodigo`: comportamiento de tasación intacto (sin regresión).
- [ ] Response sin cambios de forma; refleja el grupo elegido.
- [ ] (Opcional) validación de coherencia criterios ↔ grupo.

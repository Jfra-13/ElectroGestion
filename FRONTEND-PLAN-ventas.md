# Plan Frontend — Pantalla de Ventas / Tasación

Handoff para el equipo de frontend. El backend ya expone todo lo necesario; esto
es solo trabajo de UI. Endpoints y formas de datos verificados contra el código.

---

## 1. Objetivo

La pantalla de venta hoy está "a ciegas": el usuario escribe potencia + combustible
y el backend adivina el grupo. Sin ver el inventario, no sabe qué pedir ni si hay
stock. Se agrega un **panel contextual** que muestra los grupos disponibles con su
stock mientras se llena el formulario, y que al confirmar la venta se transforma en
el **resumen de la venta**.

---

## 2. Layout responsive

Una **única zona** (el panel contextual) que en todos los breakpoints vive en la
**parte inferior** de la pantalla, debajo del formulario:

- **Mobile**: form arriba, panel abajo (apilado vertical).
- **Tablet**: igual, apilado vertical, panel abajo.
- **Desktop**: igual, panel abajo (ancho completo bajo el form). Opcional: si sobra
  espacio horizontal, puede ir como columna lateral, pero el default acordado es abajo.

El panel NO es un componente nuevo por breakpoint: es el mismo, solo cambia el ancho.

---

## 3. Estados del panel (un solo lugar, dos estados)

### Estado A — Lista de grupos disponibles (default)
Tabla/lista con los grupos del inventario. Sirve de referencia para llenar el form.
Por cada grupo mostrar:

| Campo a mostrar | Origen (JSON) |
|---|---|
| Código | `codigo` |
| Tipo | `tipoGrupo` ("Fijo" / "Móvil") |
| Combustible | `tipoCombustible` |
| Potencia (min–max) | `pMin` – `pMax` |
| Precio unitario | `precioVentaCalculado` |
| **Stock** | `stock` |

Recomendado: resaltar en rojo / deshabilitar visualmente los grupos con `stock = 0`.

### Estado B — Resumen de la venta
Reemplaza al Estado A cuando la venta se crea con éxito. Mostrar:

| Campo | Origen (JSON respuesta de la venta) |
|---|---|
| N° de venta | `identificador` |
| Grupo asignado | `grupoCodigo` |
| Cantidad | `cantidad` |
| Precio unitario | `precioVentaUnitario` |
| **Total** | `total` |
| Vendedor | `vendedorUsername` |
| Cliente | `entidadNombre` |

---

## 4. Flujo y transiciones

```
[Estado A: lista grupos+stock]
        │  usuario llena form y confirma → POST /api/v1/ventas
        ▼
[Estado B: resumen de venta]   (la lista desaparece, aparece el resumen)
        │  botón "Nueva venta"
        ▼
[Estado A otra vez]  → form reseteado + refetch de grupos (stock actualizado)
```

**Decisión sobre "Nueva venta"** (mi recomendación): el resumen **desaparece** y el
panel vuelve al Estado A en el mismo lugar. NO reposicionar el resumen a otro lado;
queda un solo bloque que alterna entre lista y resumen. Al volver:
1. Resetear el formulario.
2. **Re-pedir la lista de grupos** (`GET`) para que el stock refleje el descuento de
   la venta recién hecha.

---

## 5. Endpoints

### GET grupos (público, sin token)
```
GET /api/v1/grupos-electrogenos?page=0&size=50&sort=codigo,asc
```
Respuesta: `PaginatedResponseDTO` → el array está en el campo de contenido paginado.
Cada item es un `GrupoElectrogenoResponseDTO` con los campos de la tabla del Estado A
(incluye `stock`, `pMax`, `pMin`, `precioVentaCalculado`, `tipoCombustible`).

### POST venta (requiere token; ROLE_ADMIN o ROLE_EMPLEADO)
```
POST /api/v1/ventas
Authorization: Bearer <jwt>
```
Body (`SolicitudCompraRequestDTO`):
```json
{
  "nombreSolicitante": "Constructora del Sur S.A.",
  "tipoPago": "EFECTIVO",
  "cantidad": 1,
  "potenciaRequerida": 18,
  "tipoCombustible": "GASOIL",
  "vidaUtilSolicitada": 8,
  "entidadId": 1
}
```
Respuesta 201 (`SolicitudCompraResponseDTO`): campos del Estado B.

**Importante**: el front NO elige el grupo. Manda combustible + potencia y el backend
asigna el grupo automáticamente.

---

## 6. Cómo elige el grupo el backend (clave para guiar al usuario)

1. Filtra grupos por `tipoCombustible`.
2. De esos, toma el de **mayor `pMax` cuyo `pMax >= potenciaRequerida`**.
3. Valida que `stock >= cantidad`.

Por eso fallaba con *"No se encontró un Grupo... que cumpla con la potencia"*: se pidió
más potencia de la que existe para ese combustible. La lista del Estado A es justamente
lo que evita ese error a ciegas: el usuario ve los `pMax` y stock reales antes de pedir.

Topes de potencia por combustible (con los datos actuales de demo):
- `NAFTA`: hasta 15
- `GASOIL`: hasta 40
- `GAS_NATURAL`: hasta 80

---

## 7. Manejo de errores (mostrar mensaje, NO pasar al resumen)

| Situación | Respuesta backend | Qué hacer en UI |
|---|---|---|
| No hay grupo para esa potencia/combustible | 404 + mensaje | Mostrar el mensaje; quedarse en Estado A |
| Stock insuficiente | 409 (`StockInsuficienteException`) | Mostrar mensaje; quedarse en Estado A |
| Validación de campos | 400 | Marcar campos inválidos |
| Sin token / token vencido | 401 | Redirigir a login |
| Rol sin permiso (ej. ROLE_USER) | 403 | Mensaje "no autorizado" |

Solo se pasa al **Estado B** con respuesta **201**.

---

## 8. Fuera de alcance

- Cambios de backend: ninguno. Todo resuelto.
- Paginación de la lista de grupos en el panel: con `size=50` alcanza para el inventario
  actual; paginar recién si crece mucho.

# Sistema de Gestión de Grupos Electrógenos

Repositorio Sistema para la gestión de solicitudes de grupos electrógenos.

## Tecnologías utilizadas
- Java 21, Spring Boot 3.4
- Spring Security + JWT
- Spring Data JPA + PostgreSQL / H2
- JaCoCo (Cobertura de Código)
- SpringDoc OpenAPI (Swagger)

## Guía de Inicio Rápido

### Requisitos
- JDK 21
- Maven (incluido como `./mvnw`)

### Ejecución
```bash
./mvnw spring-boot:run
```

### Tests y Cobertura
Para ejecutar los tests y generar el reporte de JaCoCo:
```bash
./mvnw verify
```
El reporte estará disponible en: `target/site/jacoco/index.html`. 
**Nota:** El build fallará si la cobertura de líneas es inferior al 80%.

## Contrato API (Swagger)
La documentación interactiva y el contrato OpenAPI están disponibles en:
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Docs**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Flujo de uso con Seguridad (JWT)
Para probar los endpoints protegidos (POST/PUT/DELETE) desde Swagger UI:
1. Ir al endpoint `POST /api/v1/auth/login`.
2. Ejecutar con las credenciales (por defecto en dev: `admin` / `admin123`).
3. Copiar el valor del campo `token` de la respuesta JSON.
4. En la parte superior de Swagger UI, hacer clic en el botón **Authorize**.
5. Pegar el token en el campo **Value** y pulsar **Authorize**.
6. Ahora puedes consumir los endpoints de grupos y ventas con los permisos de administrador.

## Seguridad
- **CORS**: Configurado para permitir orígenes específicos mediante la propiedad `cors.allowed-origins`. En producción, se puede sobrescribir con la variable de entorno `ALLOWED_ORIGINS` (ej. `ALLOWED_ORIGINS=https://mi-frontend.com,https://otro.com`).
- **Endpoints públicos (GET)**: `/api/v1/grupos-electrogenos/**`, `/api/v1/solicitudes-compra/**`, `/api/v1/ventas/**`.
- Endpoints protegidos (POST/PUT/DELETE): Requieren rol `ROLE_ADMIN` y Token JWT.
- Obtener Token: `POST /api/v1/auth/login` con credenciales válidas.

## Pruebas E2E
Se incluye el archivo `smoke_test.http` para realizar pruebas rápidas de los endpoints. Requiere un cliente REST que soporte el formato `.http` (como IntelliJ o VS Code REST Client).

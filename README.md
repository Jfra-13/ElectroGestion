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

## Documentación API
Una vez iniciada la aplicación, la documentación Swagger está disponible en:
- [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Seguridad
- Endpoints públicos (GET): `/api/v1/grupos-electrogenos/**`, `/api/v1/ventas/ingresos-totales` (parcial).
- Endpoints protegidos (POST/PUT/DELETE): Requieren rol `ROLE_ADMIN` y Token JWT.
- Obtener Token: `POST /api/v1/auth/login` con credenciales válidas.

## Pruebas E2E
Se incluye el archivo `smoke_test.http` para realizar pruebas rápidas de los endpoints. Requiere un cliente REST que soporte el formato `.http` (como IntelliJ o VS Code REST Client).

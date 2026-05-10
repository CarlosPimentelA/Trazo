# Trazo

Sistema de trazabilidad logística para la gestión de viajes, conductores, camiones, rutas y cargas. Diseñado para operar con múltiples consumidores (aplicación web y móvil).

---

## Tecnologías

- **Java 17**
- **Spring Boot 4**
- **Spring Security 7** con OAuth2 Resource Server
- **JWT**
- **PostgreSQL**
- **Hibernate / JPA**
- **Lombok**
- **Maven**
- **SpringDoc OpenAPI** (Swagger UI)

---

## Módulos implementados

### Autenticación (`/api/auth`)
- Registro de usuarios con verificación de email
- Login con emisión de access token (15 min) y refresh token (30 días)
- Verificación de cuenta por email
- Reenvío de link de verificación con rate limiting (3 requests/hora por email)
- Refresh token con rotación automática
- Reuse detection — invalidación total de sesión ante reutilización de token
- Logout con invalidación de todas las sesiones activas

### Viajes (`/api/travels`)
- CRUD completo de viajes
- Máquina de estados con transiciones explícitas:
  - `PENDIENTE` → `LOADING` → `START` → `STOP` → `UNLOADING` → `COMPLETE`
  - `CANCEL` disponible en cualquier estado
- Consulta de viajes activos
- Filtro por conductor y camión
- Filtro por rango de fechas
- Cálculo de peso total actual
- Actualización de costo estimado

### Conductores (`/api/drivers`)
- CRUD completo de conductores
- Consulta de conductores disponibles
- Gestión de disponibilidad

### Camiones (`/api/trucks`)
- CRUD completo de camiones
- Consulta por estado: disponibles, en uso, asignados
- Gestión de estado del camión

### Cargas (`/api/loads`)
- CRUD completo de cargas
- Asociación a viajes

### Rutas (`/api/routes`)
- CRUD completo de rutas
- Puntos de origen y destino con coordenadas (lat, lon, nombre)

### Catálogos (`/api/catalogs`)
- Gestión de tipos de camión

---

## Seguridad

- Autenticación stateless con JWT firmado con HMAC SHA-256
- Refresh tokens persistidos en base de datos con rotación y reuse detection
- Rate limiting en endpoint de reenvío de verificación con Bucket4j
- Roles: `ROLE_ADMIN`, `ROLE_DISPATCHER`, `ROLE_DRIVER`, `ROLE_CLIENT`
- Autorización por endpoint con `@PreAuthorize`

---

## Estructura del proyecto

```
src/
└── main/
    └── java/
        └── com/trucks_logistics/
            ├── auth/
            │   ├── controller/
            │   ├── service/
            │   ├── repository/
            │   ├── domain/
            │   ├── dto/
            │   └── token/
            │       ├── refresh/
            │       └── verification/
            ├── travels/
            ├── drivers/
            ├── trucks/
            ├── loads/
            ├── routes/
            ├── catalogs/
            ├── exceptions/
            └── infra/
                ├── security/
                ├── swagger/
                ├── email/
                └── ratelimit/
```

---

## Variables de entorno

```properties
# Base de datos
spring.datasource.url=jdbc:postgresql://localhost:5432/trazo
spring.datasource.username=
spring.datasource.password=

# JWT
jwt.secret=
jwt.expiration-ms=900000
jwt.refresh-expiration-days=30

# App
app.base-url=http://localhost:8080
app.verification-url=${app.base-url}/api/auth/verify?token=

# Email
spring.mail.host=
spring.mail.port=
spring.mail.username=
spring.mail.password=
```

---

## Documentación de la API

Con la aplicación corriendo, accede a Swagger UI en:

```
http://localhost:8080/swagger-ui/index.html
```
---

## Instalación y ejecución local

```bash
# Clonar el repositorio
git clone https://github.com/tu-usuario/trazo.git
cd trazo

# Configurar variables de entorno en application.properties

# Ejecutar
./mvnw spring-boot:run
```

Requiere Java 17 y PostgreSQL corriendo localmente.

---

## Licencia

Proyecto de uso privado. Todos los derechos reservados.

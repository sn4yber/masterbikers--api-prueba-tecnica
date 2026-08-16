# Master Bikers API

Servicio backend para gestionar productos y sincronizarlos desde páginas HTML de Automation Exercise mediante trabajos asíncronos.

## Funcionalidades

- CRUD REST de productos con paginación y filtros combinables.
- Persistencia en PostgreSQL.
- Extracción directa desde `https://automationexercise.com/product_details/{id}` con Jsoup.
- Procesamiento asíncrono de hasta tres productos simultáneos.
- Recuperación automática de trabajos interrumpidos al arrancar.
- Reintentos con backoff exponencial para errores de red, HTTP `429` y HTTP `5xx`.
- Idempotencia para solicitudes de extracción equivalentes.
- Aislamiento de errores por producto.
- Consulta persistida del estado, progreso y resultado de cada trabajo.
- Métricas de duración, éxito y fallo de scraping mediante Actuator y Micrometer.
- Validación de entradas y errores con formato RFC Problem Details.
- Migraciones versionadas con Flyway.
- OpenAPI y Swagger UI.
- CORS configurable para consumo desde frontend.

## Tecnologías

- Java 25
- Spring Boot 4.1
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Flyway
- Jsoup
- Micrometer y Spring Boot Actuator
- Maven
- JUnit 5, Mockito y Testcontainers
- Springdoc OpenAPI
- Docker y Docker Compose

Spring Boot permite construir una API REST con poco código de infraestructura. PostgreSQL aporta persistencia transaccional y restricciones de integridad. Jsoup permite obtener y analizar HTML sin utilizar la API pública de Automation Exercise. Flyway mantiene el esquema reproducible y verificable.

## Ejecución local

### Requisitos

- Java 25
- PostgreSQL

Crear una base de datos vacía llamada `masterbikers`:

```bash
psql -U postgres -c 'CREATE DATABASE masterbikers;'
```

Configurar credenciales como variables de entorno y ejecutar:

```bash
export DB_PASSWORD='tu-clave-local'
./mvnw spring-boot:run
```

Valores configurables:

| Variable | Valor predeterminado |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/masterbikers` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | vacío |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173,https://masterbikers.netlify.app` |
| `AUTOMATION_EXERCISE_BASE_URL` | `https://automationexercise.com` |
| `SCRAPER_TIMEOUT_MS` | `10000` |
| `SCRAPER_MAX_ATTEMPTS` | `3` |
| `SCRAPER_INITIAL_BACKOFF_MS` | `200` |
| `SCRAPER_MAX_BACKOFF_MS` | `2000` |

La contraseña no se almacena en el repositorio. Flyway aplica las migraciones al arrancar. Las migraciones crean tablas y restricciones, pero no insertan productos ni datos de demostración.

### Docker Compose

Credenciales locales se cargan desde `.env`, archivo ignorado por Git. Configurar `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD` antes de iniciar.

Iniciar backend y PostgreSQL:

```bash
docker compose up --build -d
docker compose ps
```

Ver logs del backend:

```bash
docker compose logs -f api
```

Esto inicia PostgreSQL en `localhost:${DB_PORT}` (`5433` en `.env`) y API en `localhost:8080`. Dentro de Compose, backend usa `postgres:5432`. Datos permanecen en volumen `postgres18-data` montado según estructura requerida por PostgreSQL 18.

## Documentación HTTP

Despliegue público en DigitalOcean:
- URL Fronted :https://masterbikers.netlify.app/
- URL base de la API: `http://45.55.225.78:8080`
- Swagger UI: `http://45.55.225.78:8080/swagger-ui.html`
- OpenAPI JSON: `http://45.55.225.78:8080/api-docs`
- Salud: `http://45.55.225.78:8080/actuator/health`
- Catálogo de métricas: `http://45.55.225.78:8080/actuator/metrics`

Entorno local:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- Salud: `http://localhost:8080/actuator/health`
- Catálogo de métricas: `http://localhost:8080/actuator/metrics`
- Métricas de scraping: `scraping.duration`, `scraping.success`, `scraping.failure`

## Verificación del piloto

Pruebas ejecutadas exitosamente:

- arranque de API y PostgreSQL 18 mediante Docker Compose;
- estado de salud y acceso a documentación OpenAPI;
- creación, consulta, actualización y eliminación de productos;
- paginación, ordenamiento y filtros combinables;
- validación de solicitudes inválidas y formato RFC Problem Details;
- configuración CORS para frontend local y desplegado;
- creación y procesamiento asíncrono de extracciones;
- consulta de estado e items de extracción;
- idempotencia de solicitudes de extracción equivalentes;
- persistencia de productos extraídos;
- registro de métricas de scraping.

## API de productos

| Método | Ruta | Resultado |
|---|---|---|
| `POST` | `/api/v1/products` | Crea un producto y responde `201 Created`. |
| `GET` | `/api/v1/products` | Devuelve página de productos con filtros opcionales. |
| `GET` | `/api/v1/products/{id}` | Consulta un producto. |
| `PATCH` | `/api/v1/products/{id}` | Actualiza campos enviados. |
| `DELETE` | `/api/v1/products/{id}` | Elimina un producto y responde `204 No Content`. |

Parámetros de listado:

- paginación: `page` desde `0`, `size` hasta `100` y `sort=campo,dirección`;
- texto parcial sin distinguir mayúsculas: `name`, `category`, `brand`;
- coincidencia exacta: `availability`, `condition`, `source`.

Ejemplo:

```http
GET /api/v1/products?page=0&size=20&sort=price,asc&name=bike&availability=IN_STOCK
```

La respuesta usa representación estable `PagedModel`: `content` contiene productos y `page` contiene `size`, `number`, `totalElements` y `totalPages`.

Producto manual de ejemplo:

```json
{
  "name": "Casco integral",
  "description": "Casco certificado para uso urbano",
  "price": 349900.00,
  "category": "Protección",
  "availability": "IN_STOCK",
  "condition": "NEW",
  "brand": "Ejemplo"
}
```

`externalId` y `source` son opcionales para productos manuales. Cuando están presentes, deben enviarse juntos. Productos extraídos utilizan `source: "AUTOMATION_EXERCISE"` y se actualizan por la combinación única `(source, externalId)`.

Valores permitidos:

- `availability`: `IN_STOCK`, `OUT_OF_STOCK`, `UNKNOWN`
- `condition`: `NEW`, `USED`, `REFURBISHED`, `UNKNOWN`

## API de extracciones

Crear trabajo:

```http
POST /api/v1/extractions
Content-Type: application/json
```

```json
{
  "productIds": [1, 2, 3, 4, 5]
}
```

Respuesta inmediata:

```http
HTTP/1.1 202 Accepted
Location: /api/v1/extractions/{id}
```

```json
{
  "id": "e11a3174-456f-4dc1-b495-51455fcab207",
  "status": "PENDING"
}
```

La lista ordenada de `productIds` se resume con SHA-256. Repetir una solicitud con mismos IDs, incluso en otro orden, devuelve mismo trabajo y no crea items ni tráfico de scraping adicional.

Consultar progreso:

```http
GET /api/v1/extractions/{id}
```

```json
{
  "id": "e11a3174-456f-4dc1-b495-51455fcab207",
  "status": "PROCESSING",
  "total": 5,
  "processed": 3,
  "successful": 2,
  "failed": 1,
  "createdAt": "2026-08-16T14:00:00Z",
  "startedAt": "2026-08-16T14:00:00Z",
  "finishedAt": null
}
```

Consultar resultado individual:

```http
GET /api/v1/extractions/{id}/items
```

Estados del trabajo:

```text
PENDING -> PROCESSING -> COMPLETED
                      -> COMPLETED_WITH_ERRORS
                      -> FAILED
```

Estados de cada producto:

```text
PENDING -> PROCESSING -> SUCCESS
                      -> FAILED
```

## Procesamiento asíncrono

1. Petición crea `ExtractionJob` y sus `ExtractionItem` dentro de una transacción.
2. API responde `202 Accepted` sin esperar scraping.
3. Evento se procesa después del commit mediante executor de trabajos.
4. Items se envían a executor separado con máximo tres threads.
5. Cada item marca `PROCESSING` dentro de transacción corta.
6. Solicitud HTTP externa ocurre fuera de transacción de base de datos.
7. Producto se crea o actualiza y el item termina en `SUCCESS`; errores terminan en `FAILED` sin detener demás items.
8. Estado final se calcula usando estados persistidos.

Al arrancar, trabajos `PENDING` o `PROCESSING` se recuperan. Jobs interrumpidos vuelven a `PENDING`, items que quedaron en `PROCESSING` también vuelven a `PENDING`, y procesamiento se agenda otra vez. Items ya terminados conservan resultado.

Errores de red, HTTP `429` y HTTP `5xx` se reintentan hasta límite configurado con backoff exponencial. Otros errores HTTP fallan inmediatamente. Cada operación final registra contador de éxito o fallo y duración total incluyendo reintentos.

Los contadores de progreso se consultan desde `extraction_items`; no dependen de memoria del proceso. Bloqueos pesimistas protegen transiciones iniciales frente a procesamiento duplicado.

## Modelo y migraciones

Migraciones en `src/main/resources/db/migration`:

- `V1__create_products.sql`
- `V2__create_extraction_jobs_and_items.sql`
- `V3__add_extraction_idempotency.sql`

Esquema mínimo deliberado:

- `products`
- `extraction_jobs`
- `extraction_items`

Marca se almacena como texto porque Automation Exercise entrega marcas dinámicas. Esto evita catálogos precargados y mantiene foco en requerimientos obligatorios. Proveedores y facturación quedan fuera del MVP porque no forman parte del núcleo solicitado.

## Pruebas

```bash
./mvnw test
```

Docker debe estar disponible para ejecutar pruebas PostgreSQL; si no está disponible, Testcontainers omite exclusivamente esas pruebas.

Pruebas cubren:

- validación de creación y actualización de productos;
- creación, consulta y eliminación en servicio;
- deduplicación por referencia externa;
- parsing HTML sin acceso de red;
- reintentos HTTP, clasificación de errores temporales y métricas;
- validación de solicitudes de extracción;
- hash e idempotencia de solicitudes equivalentes;
- recuperación de jobs e items interrumpidos;
- transiciones de jobs e items;
- cálculo persistido del progreso;
- migraciones, filtros y paginación sobre PostgreSQL real mediante Testcontainers.

## Manejo de errores

API responde `application/problem+json` para errores `400`, `404`, `409` y `500`. Errores inesperados se registran en servidor, pero stacktraces no se exponen al cliente. Cada fallo de scraping conserva mensaje seguro dentro del item correspondiente.

## Decisiones y trade-offs

- Executors internos evitan agregar Redis, RabbitMQ o Kafka para un servicio pequeño.
- Límite global de tres threads y reintentos acotados evitan consultas externas ilimitadas.
- Estados y resultados viven en PostgreSQL; executor solo ejecuta trabajo.
- Recuperación al arrancar cubre reinicios de una instancia. Despliegue activo-activo requeriría leases o cola durable.
- Idempotencia reutiliza indefinidamente trabajo equivalente; una futura operación explícita podría forzar resincronización.
- Filtros textuales priorizan simplicidad; catálogos grandes podrían requerir índices trigram y búsqueda dedicada.
- No se agregó autenticación porque el reto no la solicita.

## Uso de inteligencia artificial

Se utilizó Devin como apoyo para analizar requerimientos, revisar decisiones, implementar partes del código y ejecutar verificaciones. Las decisiones finales, estructura y comportamiento deben revisarse y comprenderse antes de presentar la solución.

## Mejoras completadas

- Recuperación automática de trabajos interrumpidos.
- Reintentos con backoff para errores HTTP temporales.
- Paginación y filtros de productos.
- Pruebas de integración con PostgreSQL mediante Testcontainers.
- Métricas de duración, éxito y fallo de scraping.
- Idempotencia para solicitudes de extracción equivalentes.

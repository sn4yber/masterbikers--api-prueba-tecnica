# Master Bikers API

Servicio backend para gestionar productos y sincronizarlos desde páginas HTML de Automation Exercise mediante trabajos asíncronos.

## Funcionalidades

- CRUD REST de productos.
- Persistencia en PostgreSQL.
- Extracción directa desde `https://automationexercise.com/product_details/{id}` con Jsoup.
- Procesamiento asíncrono de hasta tres productos simultáneos.
- Aislamiento de errores por producto.
- Consulta persistida del estado, progreso y resultado de cada trabajo.
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
- Maven
- JUnit 5 y Mockito
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
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` |
| `AUTOMATION_EXERCISE_BASE_URL` | `https://automationexercise.com` |
| `SCRAPER_TIMEOUT_MS` | `10000` |

La contraseña no se almacena en el repositorio. Flyway aplica las migraciones al arrancar. Las migraciones crean tablas y restricciones, pero no insertan productos ni datos de demostración.

### Docker Compose

```bash
export DB_PASSWORD='tu-clave-local'
docker compose up --build
```

Esto inicia PostgreSQL y la API. Los datos de PostgreSQL permanecen en el volumen `postgres-data`.

## Documentación HTTP

Con la aplicación ejecutándose:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## API de productos

| Método | Ruta | Resultado |
|---|---|---|
| `POST` | `/api/v1/products` | Crea un producto y responde `201 Created`. |
| `GET` | `/api/v1/products` | Lista productos. |
| `GET` | `/api/v1/products/{id}` | Consulta un producto. |
| `PATCH` | `/api/v1/products/{id}` | Actualiza campos enviados. |
| `DELETE` | `/api/v1/products/{id}` | Elimina un producto y responde `204 No Content`. |

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

Los contadores de progreso se consultan desde `extraction_items`; no dependen de memoria del proceso. Bloqueos pesimistas protegen transiciones iniciales frente a procesamiento duplicado.

## Modelo y migraciones

Migraciones en `src/main/resources/db/migration`:

- `V1__create_products.sql`
- `V2__create_extraction_jobs_and_items.sql`

Esquema mínimo deliberado:

- `products`
- `extraction_jobs`
- `extraction_items`

Marca se almacena como texto porque Automation Exercise entrega marcas dinámicas. Esto evita catálogos precargados y mantiene foco en requerimientos obligatorios. Proveedores y facturación quedan fuera del MVP porque no forman parte del núcleo solicitado.

## Pruebas

```bash
./mvnw test
```

Pruebas cubren:

- validación de creación y actualización de productos;
- creación, consulta y eliminación en servicio;
- deduplicación por referencia externa;
- parsing HTML sin acceso de red;
- validación de solicitudes de extracción;
- transiciones de jobs e items;
- cálculo persistido del progreso.

## Manejo de errores

API responde `application/problem+json` para errores `400`, `404`, `409` y `500`. Errores inesperados se registran en servidor, pero stacktraces no se exponen al cliente. Cada fallo de scraping conserva mensaje seguro dentro del item correspondiente.

## Decisiones y trade-offs

- Executors internos evitan agregar Redis, RabbitMQ o Kafka para un servicio pequeño y una prueba de tres días.
- Límite global de tres threads evita consultas externas ilimitadas.
- Estados y resultados viven en PostgreSQL; executor solo ejecuta trabajo.
- Reinicio durante procesamiento puede dejar jobs en `PROCESSING`. En producción se agregaría recuperación de jobs incompletos al arrancar o una cola durable.
- Lista de productos no tiene paginación en MVP. Se agregaría antes de manejar catálogos grandes.
- No se agregó autenticación porque el reto no la solicita.
- No se agregaron reintentos automáticos para evitar duplicar tráfico externo; serían mejora posterior con backoff para errores temporales.

## Uso de inteligencia artificial

Se utilizó Devin como apoyo para analizar requerimientos, revisar decisiones, implementar partes del código y ejecutar verificaciones. Las decisiones finales, estructura y comportamiento deben revisarse y comprenderse antes de presentar la solución.

## Mejoras con más tiempo

- Recuperación automática de trabajos interrumpidos.
- Reintentos con backoff para errores HTTP temporales.
- Paginación y filtros de productos.
- Pruebas de integración con PostgreSQL mediante Testcontainers.
- Métricas de duración, éxito y fallo de scraping.
- Idempotencia para solicitudes de extracción equivalentes.

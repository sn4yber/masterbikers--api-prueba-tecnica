# MASTER BIKERS — Levantamiento de Requerimientos (consolidado)

Prueba técnica — Ingeniero de Software Jr. Ventana de entrega: **3 días calendario**.
Repositorio en GitHub con colaborador: **gruporedoficial**.

> Documento de análisis y diseño inicial. La implementación final prioriza el alcance P0 del reto.
> `README.md` y las migraciones Flyway describen el comportamiento ejecutable. Marcas como entidad,
> proveedores y facturación permanecen como posibles extensiones, no como parte del MVP implementado.

---

## 1. Descripción del proyecto

Backend para la gestión de un catálogo de productos y repuestos de motocicletas, que permite
administrar productos, marcas y proveedores, y sincronizar información de productos desde una
fuente externa (Automation Exercise) mediante procesos **asíncronos**. Como funcionalidad
complementaria, genera una representación visual de facturas en formato PNG.

## 2. Objetivo principal

- Gestionar el catálogo de productos (CRUD).
- Gestionar marcas y proveedores.
- Consultar y actualizar productos desde una fuente externa (scraping HTML, **no API pública**).
- Ejecutar extracciones de múltiples productos de manera asíncrona, con control de concurrencia.
- Registrar progreso, resultado y errores de cada extracción.
- Persistir toda la información en PostgreSQL.
- (Opcional / extra) Generar una representación visual de factura en PNG.

## 3. Actores

| Actor | Responsabilidades |
|---|---|
| **Administrador** | Administrar productos, consultar catálogo, actualizar/eliminar productos, iniciar sincronizaciones, consultar trabajos de extracción, generar facturas. |
| **Sistema externo (Automation Exercise)** | Fuente externa usada exclusivamente para la extracción HTML (`automationexercise.com/product_details/{id}`). No se usa su API pública. |

## 4. Alcance del MVP

```
PRODUCTOS ──CRUD──▶ POSTGRESQL ◀──EXTRACCIONES── AUTOMATION EXERCISE
                                  (crear job, procesar async,
                                   controlar concurrencia,
                                   registrar errores,
                                   consultar progreso)
```

Facturación, CRUD completo de proveedores, paginación y filtros avanzados son **complementarios**,
no el núcleo evaluable.

---

## 5. Requerimientos funcionales

### 5.1 Gestión de productos

| ID | Endpoint | Descripción |
|---|---|---|
| RF-01 | `POST /api/v1/products` | Crear producto (nombre, descripción, precio, categoría, disponibilidad, condición, marca, URL de origen, identificador externo). |
| RF-02 | `GET /api/v1/products` | Listar catálogo (paginación/filtros `page`, `size`, `brand`, `category` no son prioridad inicial). |
| RF-03 | `GET /api/v1/products/{id}` | Consultar un producto específico. |
| RF-04 | `PATCH /api/v1/products/{id}` | Actualizar producto. |
| RF-05 | `DELETE /api/v1/products/{id}` | Eliminar producto. |

### 5.2 Marcas

- RF-06 — El sistema debe permitir asociar un producto a una marca (`Brand ≠ Supplier`: una marca
  fabrica/comercializa; un proveedor puede vender productos de varias marcas).
- Marcas iniciales (seed): Honda, Yamaha, Suzuki, BMW Motorrad, CFMOTO, AKT, Bajaj.
- `GET /api/v1/brands` — listado (no requiere CRUD completo para el MVP).

### 5.3 Proveedores

- RF-07 — El sistema debe permitir asociar productos con proveedores.
- No se requiere CRUD completo de proveedores en el MVP si consume tiempo del núcleo evaluable;
  se deja la estructura de dominio preparada (`supplier_id` nullable en `Product`).
- `GET /api/v1/suppliers` — listado.

### 5.4 Extracción de productos (funcionalidad central)

| ID | Endpoint | Descripción |
|---|---|---|
| RF-08 | `POST /api/v1/extractions` | Crea un trabajo de extracción a partir de `{ "productIds": [1,2,3,4,5] }`. Responde `202 Accepted` con `{ "id", "status": "PENDING" }` **sin esperar** a que termine el procesamiento. |
| RF-09 | — | El procesamiento no debe bloquear la petición HTTP (asíncrono en background). |
| RF-10 | — | Límite de concurrencia contra la fuente externa (propuesta: **máx. 3 productos simultáneos**). |
| RF-11 | — | El fallo de un producto no detiene el resto del job; el job puede terminar como `COMPLETED_WITH_ERRORS`. |
| RF-12 | `GET /api/v1/extractions/{id}` | Consulta estado/progreso: `{ id, status, total, processed, successful, failed }`. |
| RF-13 | `GET /api/v1/extractions/{id}/items` | Resultado por producto: `[{ externalProductId, status, errorMessage? }]`. |
| RF-14 | — | Scraping HTML directo de `automationexercise.com/product_details/{id}` (no la API pública). Debe tolerar campos ausentes. Campos a extraer: `externalId, name, price, category, availability, condition, brand, sourceUrl`. |
| RF-15 | — | Toda la información relevante debe persistirse en PostgreSQL (no solo en memoria). |

**Estados del job (`ExtractionJob.status`):** `PENDING → PROCESSING → COMPLETED | COMPLETED_WITH_ERRORS | FAILED`
**Estados del item (`ExtractionItem.status`):** `PENDING → PROCESSING → SUCCESS | FAILED`

### 5.5 Facturación (extra / complementario)

| ID | Endpoint | Descripción |
|---|---|---|
| RF-16 | `POST /api/v1/invoices` | Crea factura con cliente, documento, fecha, productos, cantidades; el backend calcula subtotal, impuestos y total. Se guarda `unit_price` histórico (no depende del precio actual del producto). |
| RF-17 | `GET /api/v1/invoices/{id}/png` | Genera representación visual de la factura en PNG (no es facturación electrónica DIAN). El PNG se genera on-demand, no se almacena como blob en BD. |

---

## 6. Requerimientos no funcionales

| ID | Requerimiento |
|---|---|
| RNF-01 | Arquitectura modular con Spring Boot, organizada por *feature* (product, brand, supplier, extraction, scraper, invoice, common). |
| RNF-02 | Persistencia en PostgreSQL. |
| RNF-03 | API REST con códigos HTTP correctos: 200, 201, 202, 204, 400, 404, 409, 500. |
| RNF-04 | Validación de requests (`price > 0`, `name` obligatorio, `quantity > 0`, etc.). |
| RNF-05 | Manejo de errores estructurado (sin stacktraces expuestos al cliente). |
| RNF-06 | Concurrencia del scraper limitada (propuesta: 3 workers). |
| RNF-07 | Historial de Git organizado y significativo. |
| RNF-08 | Ejecutable vía Docker Compose. |

---

## 7. Decisiones técnicas cerradas

### 7.1 Stack

Java 25 · Spring Boot 3.5.x · Spring Web · Spring Data JPA · Bean Validation · PostgreSQL ·
Jsoup (scraping) · Flyway (migraciones) · Maven · JUnit 5 + Mockito · Docker / Docker Compose ·
OpenAPI / Swagger.

Explícitamente **fuera de alcance**: Spring Security/JWT (no se pidió autenticación),
Redis/Kafka/RabbitMQ/microservicios (sobreingeniería para esta prueba), Lombok inicialmente
(se prefiere código explícito y fácil de defender en la entrevista).

### 7.2 Arquitectura por capas (feature-based)

```
src/main/java/com/masterbikers
├── product/       (controller, service, repository, entity, dto, mapper)
├── brand/         (controller, service, repository, entity)
├── supplier/      (controller, service, repository, entity)
├── extraction/    (controller, service, repository, entity, dto)
├── scraper/       (AutomationExerciseScraper, ProductParser)
├── invoice/       (controller, service, repository, entity, dto)
└── common/        (exception, config, response)
```

### 7.3 Convenciones de datos

- **Identificadores:** `UUID` para todas las entidades de negocio (no exponer IDs secuenciales).
- **Dinero:** `BigDecimal` (nunca `double`/`float`).
- **Fechas:** `Instant` para timestamps internos (evita problemas de zona horaria).
- **Deduplicación externa:** `UNIQUE(source, external_id)` en `products` — permite que distintas
  fuentes reutilicen el mismo `external_id` sin colisionar.
- **Migraciones versionadas con Flyway** (`V1__create_brands.sql`, `V2__...`) en vez de
  `spring.jpa.hibernate.ddl-auto=update`, para que el repositorio sea reproducible por el evaluador.

### 7.4 Endpoints definitivos

```
Products      POST/GET /api/v1/products, GET/PATCH/DELETE /api/v1/products/{id}
Extractions   POST /api/v1/extractions, GET /api/v1/extractions/{id}, GET /api/v1/extractions/{id}/items
Invoices      POST /api/v1/invoices, GET /api/v1/invoices/{id}, GET /api/v1/invoices/{id}/png
Brands        GET /api/v1/brands
Suppliers     GET /api/v1/suppliers
```

### 7.5 Spring Initializr

- **Project:** Maven · **Language:** Java · **Spring Boot:** 3.5.x (o 4.x estable compatible con Java 25)
- **Group:** `com.masterbikers` · **Artifact/Name:** `master-bikers` · **Package:** `com.masterbikers`
- **Packaging:** Jar · **Java:** 25
- **Dependencias obligatorias:** Spring Web, Spring Data JPA, PostgreSQL Driver, Validation
- **Recomendadas:** Spring Boot DevTools, Spring Boot Actuator
- **Agregar después vía `pom.xml`:** Springdoc OpenAPI/Swagger, Flyway
- **No seleccionar:** Spring Security, HATEOAS, Batch, Cloud, Kafka, RabbitMQ, Redis, Thymeleaf, Lombok (por ahora)

---

## 8. Modelo de datos

Ver diagramas adjuntos:
- `master-bikers-erd.drawio` — Diagrama Entidad-Relación (7 tablas: `brands`, `suppliers`,
  `products`, `extraction_jobs`, `extraction_items`, `invoices`, `invoice_items`).
- `master-bikers-class-diagram.drawio` — Diagrama de clases (entidades JPA + enums:
  `Availability`, `ProductCondition`, `ExtractionStatus`, `ExtractionItemStatus`).
- Carpeta `sql/` — migraciones Flyway `V1` a `V8` con el schema completo + seed de marcas.

Relaciones clave:
- `Brand 1 ─── N Product` (obligatoria)
- `Supplier 1 ─── N Product` (opcional, `supplier_id` nullable)
- `ExtractionJob 1 ─── N ExtractionItem`
- `Product 1 ─── N ExtractionItem` (`product_id` nullable — puede ser `NULL` si la extracción falló)
- `Invoice 1 ─── N InvoiceItem`
- `Product 1 ─── N InvoiceItem`

---

## 9. Priorización para los 3 días

### 🔴 P0 — Obligatorio (núcleo evaluable)
Spring Boot · PostgreSQL · Product CRUD · JPA · DTOs · Validation · Exception handling ·
HTML scraper · ExtractionJob · ExtractionItem · Async processing · Concurrency limit ·
Extraction status · Git · README.

### 🟠 P1 — Muy recomendable
Tests · Docker Compose · Swagger · Paginación · Retry · Seed de marcas.

### 🟢 P2 — Extra (no arriesgar el núcleo por esto)
Suppliers CRUD completo · Invoices · Factura PNG · Filtros avanzados.

> Regla de oro: si llega el día 3 y el núcleo (Product CRUD, scraping, async, persistencia,
> manejo de errores, tests, Docker, README) está listo, **no** empezar la factura si eso
> amenaza la estabilidad de lo ya evaluado.

---

## 10. Entregables (según el reto)

1. Repositorio en GitHub con `gruporedoficial` como colaborador.
2. Código fuente + historial de Git significativo.
3. `README.md` con: cómo ejecutar la app, tecnologías utilizadas y por qué, descripción general,
   estrategia de procesamiento asíncrono, decisiones/trade-offs relevantes, uso de IA (si aplica),
   qué mejoraría con más tiempo.

## 11. Criterios de evaluación (según el reto)

Comprensión del problema · funcionamiento de la solución · diseño y organización del código ·
uso adecuado de REST y persistencia · manejo de asincronismo · manejo de errores y estados ·
capacidad de extraer/transformar información externa · claridad de decisiones técnicas ·
uso razonable de tecnologías (no se premia usar más por usar más) · calidad del historial y
documentación del repo.

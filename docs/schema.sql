-- ============================================================
-- MASTER BIKERS - Schema de base de datos (PostgreSQL)
-- API backend consumida por un frontend
-- ============================================================

-- Extensión necesaria para generar UUID (si se requiere en BD)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- TABLA: brands (marcas)
-- ============================================================
CREATE TABLE brands (
    id          UUID PRIMARY KEY,
    nombre      VARCHAR(120) NOT NULL,
    codigo      VARCHAR(60)  NOT NULL,
    creado_en   TIMESTAMP    NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_brands_codigo UNIQUE (codigo)
);

-- ============================================================
-- TABLA: suppliers (proveedores)
-- ============================================================
CREATE TABLE suppliers (
    id             UUID PRIMARY KEY,
    nombre         VARCHAR(150) NOT NULL,
    email          VARCHAR(150),
    telefono       VARCHAR(30),
    sitio_web      VARCHAR(200),
    activo         BOOLEAN      NOT NULL DEFAULT true,
    creado_en      TIMESTAMP    NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMP    NOT NULL DEFAULT now()
);

-- ============================================================
-- TABLA: products (productos)
-- ============================================================
CREATE TABLE products (
    id                UUID PRIMARY KEY,
    id_externo        VARCHAR(60)     NOT NULL,
    fuente            VARCHAR(60)     NOT NULL,
    nombre            VARCHAR(200)    NOT NULL,
    descripcion       TEXT,
    precio            NUMERIC(12,2)   NOT NULL CHECK (precio >= 0),
    categoria         VARCHAR(120),
    disponibilidad    VARCHAR(30)     NOT NULL DEFAULT 'DESCONOCIDA',
    condicion         VARCHAR(30)     NOT NULL DEFAULT 'DESCONOCIDA',
    url_origen        VARCHAR(500),
    brand_id          UUID            NOT NULL REFERENCES brands(id),
    supplier_id       UUID            NULL REFERENCES suppliers(id),
    creado_en         TIMESTAMP       NOT NULL DEFAULT now(),
    actualizado_en    TIMESTAMP       NOT NULL DEFAULT now(),
    CONSTRAINT uq_products_fuente_id_externo UNIQUE (fuente, id_externo),
    CONSTRAINT chk_products_disponibilidad CHECK (disponibilidad IN ('DISPONIBLE','AGOTADO','DESCONOCIDA')),
    CONSTRAINT chk_products_condicion CHECK (condicion IN ('NUEVO','USADO','REACONDICIONADO','DESCONOCIDA'))
);

CREATE INDEX idx_products_brand_id ON products(brand_id);
CREATE INDEX idx_products_supplier_id ON products(supplier_id);

-- ============================================================
-- TABLA: extraction_jobs (trabajos de extracción)
-- ============================================================
CREATE TABLE extraction_jobs (
    id            UUID PRIMARY KEY,
    estado        VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE',
    total         INTEGER      NOT NULL DEFAULT 0,
    procesados    INTEGER      NOT NULL DEFAULT 0,
    exitosos      INTEGER      NOT NULL DEFAULT 0,
    fallidos      INTEGER      NOT NULL DEFAULT 0,
    creado_en     TIMESTAMP    NOT NULL DEFAULT now(),
    iniciado_en   TIMESTAMP    NULL,
    finalizado_en TIMESTAMP    NULL,
    CONSTRAINT chk_extraction_jobs_estado
        CHECK (estado IN ('PENDIENTE','PROCESANDO','COMPLETADO','COMPLETADO_CON_ERRORES','FALLIDO'))
);

-- ============================================================
-- TABLA: extraction_items (items de cada trabajo de extracción)
-- ============================================================
CREATE TABLE extraction_items (
    id                    UUID PRIMARY KEY,
    extraction_job_id     UUID         NOT NULL REFERENCES extraction_jobs(id) ON DELETE CASCADE,
    id_producto_externo   VARCHAR(60)  NOT NULL,
    product_id            UUID         NULL REFERENCES products(id),
    estado                VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE',
    mensaje_error         VARCHAR(500) NULL,
    procesado_en          TIMESTAMP    NULL,
    CONSTRAINT chk_extraction_items_estado
        CHECK (estado IN ('PENDIENTE','PROCESANDO','EXITOSO','FALLIDO'))
);

CREATE INDEX idx_extraction_items_job_id ON extraction_items(extraction_job_id);
CREATE INDEX idx_extraction_items_product_id ON extraction_items(product_id);

-- ============================================================
-- TABLA: invoices (facturas)
-- ============================================================
CREATE TABLE invoices (
    id                 UUID PRIMARY KEY,
    numero_factura     VARCHAR(40)    NOT NULL,
    nombre_cliente     VARCHAR(150)   NOT NULL,
    documento_cliente  VARCHAR(40)    NOT NULL,
    subtotal           NUMERIC(12,2)  NOT NULL CHECK (subtotal >= 0),
    impuesto           NUMERIC(12,2)  NOT NULL CHECK (impuesto >= 0),
    total              NUMERIC(12,2)  NOT NULL CHECK (total >= 0),
    creado_en          TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT uq_invoices_numero_factura UNIQUE (numero_factura)
);

-- ============================================================
-- TABLA: invoice_items (items de cada factura)
-- ============================================================
CREATE TABLE invoice_items (
    id             UUID           PRIMARY KEY,
    invoice_id     UUID           NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    product_id     UUID           NOT NULL REFERENCES products(id),
    cantidad       INTEGER        NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(12,2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal       NUMERIC(12,2)  NOT NULL CHECK (subtotal >= 0)
);

CREATE INDEX idx_invoice_items_invoice_id ON invoice_items(invoice_id);
CREATE INDEX idx_invoice_items_product_id ON invoice_items(product_id);

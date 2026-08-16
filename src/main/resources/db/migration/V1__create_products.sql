CREATE TABLE products (
    id UUID PRIMARY KEY,
    external_id VARCHAR(255),
    source VARCHAR(50),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    category VARCHAR(100),
    availability VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN'
        CHECK (availability IN ('IN_STOCK', 'OUT_OF_STOCK', 'UNKNOWN')),
    condition VARCHAR(20) NOT NULL DEFAULT 'NEW'
        CHECK (condition IN ('NEW', 'USED', 'REFURBISHED', 'UNKNOWN')),
    brand VARCHAR(100),
    source_url VARCHAR(2048),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT products_external_reference_pair_check CHECK (
        (external_id IS NULL AND source IS NULL)
        OR (external_id IS NOT NULL AND source IS NOT NULL)
    )
);

CREATE UNIQUE INDEX products_source_external_id_unique
    ON products (source, external_id)
    WHERE source IS NOT NULL AND external_id IS NOT NULL;

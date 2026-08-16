CREATE TABLE extraction_jobs (
    id UUID PRIMARY KEY,
    status VARCHAR(30) NOT NULL
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED')),
    total INTEGER NOT NULL CHECK (total > 0),
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ
);

CREATE TABLE extraction_items (
    id UUID PRIMARY KEY,
    extraction_job_id UUID NOT NULL REFERENCES extraction_jobs(id) ON DELETE CASCADE,
    external_product_id VARCHAR(255) NOT NULL,
    product_id UUID REFERENCES products(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED')),
    error_message VARCHAR(1000),
    processed_at TIMESTAMPTZ,
    CONSTRAINT extraction_items_job_external_product_unique
        UNIQUE (extraction_job_id, external_product_id)
);

CREATE INDEX extraction_items_job_status_idx
    ON extraction_items (extraction_job_id, status);

CREATE INDEX extraction_items_product_id_idx
    ON extraction_items (product_id)
    WHERE product_id IS NOT NULL;

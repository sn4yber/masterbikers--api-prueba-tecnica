ALTER TABLE extraction_jobs
    ADD COLUMN request_hash VARCHAR(64);

UPDATE extraction_jobs
SET request_hash = md5(id::text || ':legacy:1') || md5(id::text || ':legacy:2');

ALTER TABLE extraction_jobs
    ALTER COLUMN request_hash SET NOT NULL,
    ADD CONSTRAINT extraction_jobs_request_hash_unique UNIQUE (request_hash);

CREATE INDEX extraction_jobs_recovery_status_idx
    ON extraction_jobs (status)
    WHERE status IN ('PENDING', 'PROCESSING');

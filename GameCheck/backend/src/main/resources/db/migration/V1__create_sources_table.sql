CREATE TABLE sources (
    source_id SERIAL PRIMARY KEY,
    source_name VARCHAR(100) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    CONSTRAINT chk_sources_type CHECK (source_type IN ('physical', 'digital')),
    source_url TEXT NOT NULL
);

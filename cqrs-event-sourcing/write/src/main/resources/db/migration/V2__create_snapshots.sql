CREATE TABLE snapshot (
    id             BIGSERIAL PRIMARY KEY,
    aggregate_id   UUID                     NOT NULL,
    aggregate_type VARCHAR(255)             NOT NULL,
    version        INTEGER                  NOT NULL,
    state          TEXT                     NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_snapshot_aggregate_version UNIQUE (aggregate_id, version)
);

CREATE INDEX idx_snapshot_aggregate ON snapshot (aggregate_id, aggregate_type);
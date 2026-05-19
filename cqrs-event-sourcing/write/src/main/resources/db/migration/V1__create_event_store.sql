CREATE TABLE event_store (
    id          BIGSERIAL PRIMARY KEY,
    stream_id   VARCHAR(255)             NOT NULL,
    version     INTEGER                  NOT NULL,
    event_type  VARCHAR(255)             NOT NULL,
    payload     TEXT                     NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    stored_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_event_stream_version UNIQUE (stream_id, version)
);

CREATE INDEX idx_event_store_stream_id ON event_store (stream_id);
CREATE INDEX idx_event_store_stream_version ON event_store (stream_id, version);
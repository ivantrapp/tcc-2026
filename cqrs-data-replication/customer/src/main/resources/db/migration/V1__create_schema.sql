CREATE TABLE customers (
    id                 UUID PRIMARY KEY,
    name               TEXT        NOT NULL,
    address            TEXT,
    age                INT         NOT NULL,
    account_manager_id UUID,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_account_manager ON customers (account_manager_id);

CREATE TABLE customer_view (
    id                   UUID PRIMARY KEY,
    name                 VARCHAR(255) NOT NULL,
    address              VARCHAR(255),
    age                  INT,
    account_manager_id   UUID,
    account_manager_name VARCHAR(255),
    wallet_id            UUID,
    wallet_balance       NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ   NOT NULL
);

CREATE TABLE account_manager_view (
    id             UUID PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    branch_id      UUID,
    is_admin       BOOLEAN     NOT NULL DEFAULT FALSE,
    customer_count INT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL
);

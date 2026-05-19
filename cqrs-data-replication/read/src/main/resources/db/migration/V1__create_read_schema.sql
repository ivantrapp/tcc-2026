-- ============================================================
-- Tabelas replicadas via WAL — sem foreign keys intencionalmente.
-- O PostgreSQL pode entregar linhas em qualquer ordem; FKs
-- causariam violação de integridade referencial na entrega.
-- ============================================================

CREATE TABLE branches (
    id         UUID PRIMARY KEY,
    region     TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE account_managers (
    id         UUID PRIMARY KEY,
    name       TEXT        NOT NULL,
    branch_id  UUID,
    is_admin   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE customers (
    id                 UUID PRIMARY KEY,
    name               TEXT        NOT NULL,
    address            TEXT,
    age                INT         NOT NULL,
    account_manager_id UUID,
    created_at         TIMESTAMPTZ NOT NULL
);

CREATE TABLE wallets (
    id          UUID           PRIMARY KEY,
    customer_id UUID,
    balance     NUMERIC(18, 2) NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL
);

CREATE TABLE transactions (
    id                       UUID           PRIMARY KEY,
    wallet_id                UUID           NOT NULL,
    value                    NUMERIC(18, 2) NOT NULL,
    type                     TEXT           NOT NULL,
    exchange_confirmation_id UUID           NOT NULL,
    occurred_at              TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_r_customers_manager   ON customers (account_manager_id);
CREATE INDEX idx_r_wallets_customer    ON wallets (customer_id);
CREATE INDEX idx_r_transactions_wallet ON transactions (wallet_id);

-- ============================================================
-- Views de leitura — substituem as projeções em código do ES
-- ============================================================

CREATE VIEW customer_view AS
SELECT
    c.id                        AS customer_id,
    c.name                      AS customer_name,
    c.age                       AS customer_age,
    c.address                   AS customer_address,
    am.id                       AS account_manager_id,
    am.name                     AS manager_name,
    am.is_admin                 AS manager_is_admin,
    b.id                        AS branch_id,
    b.region                    AS branch_region,
    COALESCE(SUM(w.balance), 0) AS total_balance,
    (
        SELECT MAX(t.occurred_at)
        FROM transactions t
        JOIN wallets tw ON tw.id = t.wallet_id
        WHERE tw.customer_id = c.id
    )                           AS last_transaction_at
FROM customers c
LEFT JOIN account_managers am ON am.id = c.account_manager_id
LEFT JOIN branches b          ON b.id = am.branch_id
LEFT JOIN wallets w            ON w.customer_id = c.id
GROUP BY c.id, c.name, c.age, c.address,
         am.id, am.name, am.is_admin,
         b.id, b.region;

CREATE VIEW customer_transactions_view AS
SELECT
    c.id          AS customer_id,
    w.id          AS wallet_id,
    w.balance     AS wallet_balance,
    t.id          AS transaction_id,
    t.value       AS transaction_value,
    t.type        AS transaction_type,
    t.occurred_at AS transaction_occurred_at
FROM customers c
JOIN wallets     w ON w.customer_id = c.id
LEFT JOIN transactions t ON t.wallet_id = w.id
ORDER BY t.occurred_at DESC;

-- Projecoes com os mesmos campos e regras da VIEW customer_view do data replication:
-- dados atuais do gerente e da filial, soma dos saldos de todas as carteiras do
-- cliente e data da ultima transacao. Projecoes sao descartaveis (reconstruidas
-- a partir dos eventos), por isso as tabelas sao recriadas.

DROP TABLE IF EXISTS customer_view;
DROP TABLE IF EXISTS account_manager_view;

CREATE TABLE branch_view (
    id         UUID PRIMARY KEY,
    region     TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE account_manager_view (
    id         UUID PRIMARY KEY,
    name       TEXT        NOT NULL,
    branch_id  UUID,
    is_admin   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE wallet_view (
    id          UUID           PRIMARY KEY,
    customer_id UUID           NOT NULL,
    balance     NUMERIC(18, 2) NOT NULL DEFAULT 0
);

CREATE TABLE customer_view (
    customer_id         UUID PRIMARY KEY,
    customer_name       TEXT           NOT NULL,
    customer_age        INT            NOT NULL,
    customer_address    TEXT,
    account_manager_id  UUID,
    manager_name        TEXT,
    manager_is_admin    BOOLEAN        NOT NULL DEFAULT FALSE,
    branch_id           UUID,
    branch_region       TEXT,
    total_balance       NUMERIC(18, 2) NOT NULL DEFAULT 0,
    last_transaction_at TIMESTAMPTZ
);

-- Mesmos indices do read do data replication.
CREATE INDEX idx_customer_view_manager ON customer_view (account_manager_id);
CREATE INDEX idx_wallet_view_customer  ON wallet_view (customer_id);

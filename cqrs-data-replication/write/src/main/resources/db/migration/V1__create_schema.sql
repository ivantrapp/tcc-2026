CREATE TABLE branches (
    id         UUID PRIMARY KEY,
    region     TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE account_managers (
    id         UUID PRIMARY KEY,
    name       TEXT        NOT NULL,
    branch_id  UUID        REFERENCES branches (id),
    is_admin   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE customers (
    id                 UUID PRIMARY KEY,
    name               TEXT        NOT NULL,
    address            TEXT,
    age                INT         NOT NULL,
    account_manager_id UUID        REFERENCES account_managers (id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE wallets (
    id          UUID           PRIMARY KEY,
    customer_id UUID           REFERENCES customers (id),
    balance     NUMERIC(18, 2) NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE transactions (
    id                       UUID           PRIMARY KEY,
    wallet_id                UUID           NOT NULL REFERENCES wallets (id),
    value                    NUMERIC(18, 2) NOT NULL,
    type                     TEXT           NOT NULL,
    exchange_confirmation_id UUID           NOT NULL,
    occurred_at              TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_customers_account_manager ON customers (account_manager_id);
CREATE INDEX idx_wallets_customer           ON wallets (customer_id);
CREATE INDEX idx_transactions_wallet        ON transactions (wallet_id);
CREATE INDEX idx_transactions_occurred_at   ON transactions (occurred_at DESC);

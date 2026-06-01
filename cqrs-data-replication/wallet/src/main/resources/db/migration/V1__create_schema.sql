CREATE TABLE wallets (
    id          UUID           PRIMARY KEY,
    customer_id UUID,
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

CREATE INDEX idx_wallets_customer         ON wallets (customer_id);
CREATE INDEX idx_transactions_wallet      ON transactions (wallet_id);
CREATE INDEX idx_transactions_occurred_at ON transactions (occurred_at DESC);

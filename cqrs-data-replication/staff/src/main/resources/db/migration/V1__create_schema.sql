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

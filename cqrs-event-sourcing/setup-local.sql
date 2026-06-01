-- =============================================================
-- Setup do banco local — execute uma vez como superusuário:
--   psql -U postgres -p 5440 -f setup-local.sql   (write + read)
--   psql -U postgres -p 5441 -f setup-local.sql   (staff)
--   psql -U postgres -p 5442 -f setup-local.sql   (customer)
--   psql -U postgres -p 5443 -f setup-local.sql   (wallet)
--
-- Após rodar este script, suba as apps normalmente.
-- O Flyway cria as tabelas automaticamente no primeiro start.
-- =============================================================

-- ---- Instância principal (porta 5440): write + read ----

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'crm') THEN
        CREATE USER crm WITH PASSWORD 'crm';
    ELSE
        ALTER USER crm WITH PASSWORD 'crm';
    END IF;
END
$$;

SELECT 'CREATE DATABASE crm_write OWNER crm'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'crm_write') \gexec

SELECT 'CREATE DATABASE crm_read OWNER crm'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'crm_read') \gexec

GRANT ALL PRIVILEGES ON DATABASE crm_write TO crm;
GRANT ALL PRIVILEGES ON DATABASE crm_read TO crm;

\c crm_write
GRANT ALL ON SCHEMA public TO crm;

\c crm_read
GRANT ALL ON SCHEMA public TO crm;

-- ---- Instância staff (porta 5441) ----
-- psql -U postgres -p 5441 -c "CREATE USER crm WITH PASSWORD 'crm';"
-- psql -U postgres -p 5441 -c "CREATE DATABASE crm_staff OWNER crm;"
-- psql -U postgres -p 5441 -c "GRANT ALL PRIVILEGES ON DATABASE crm_staff TO crm;"

-- ---- Instância customer (porta 5442) ----
-- psql -U postgres -p 5442 -c "CREATE USER crm WITH PASSWORD 'crm';"
-- psql -U postgres -p 5442 -c "CREATE DATABASE crm_customer OWNER crm;"
-- psql -U postgres -p 5442 -c "GRANT ALL PRIVILEGES ON DATABASE crm_customer TO crm;"

-- ---- Instância wallet (porta 5443) ----
-- psql -U postgres -p 5443 -c "CREATE USER crm WITH PASSWORD 'crm';"
-- psql -U postgres -p 5443 -c "CREATE DATABASE crm_wallet OWNER crm;"
-- psql -U postgres -p 5443 -c "GRANT ALL PRIVILEGES ON DATABASE crm_wallet TO crm;"

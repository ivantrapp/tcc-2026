-- =============================================================
-- Setup do banco local — execute uma vez como superusuário:
--   psql -U postgres -p 5440 -f setup-local.sql
--
-- Após rodar este script, suba as apps normalmente.
-- O Flyway cria as tabelas automaticamente no primeiro start.
-- =============================================================

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

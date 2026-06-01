-- Runs inside crm_staff on first container start (as postgres superuser)

-- Application user
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'crm') THEN
        CREATE USER crm WITH PASSWORD 'crm';
    END IF;
END
$$;

GRANT ALL PRIVILEGES ON DATABASE crm_staff TO crm;
GRANT ALL ON SCHEMA public TO crm;

-- Replication user (used by crm_read subscription)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'replicator') THEN
        CREATE USER replicator WITH REPLICATION PASSWORD 'replicator';
    END IF;
END
$$;

-- Grant SELECT on all tables that crm will create (via Flyway) to replicator
ALTER DEFAULT PRIVILEGES FOR ROLE crm IN SCHEMA public GRANT SELECT ON TABLES TO replicator;

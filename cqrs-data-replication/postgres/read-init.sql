-- Runs inside crm_read on first container start (as postgres superuser)

-- Application user
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'crm') THEN
        CREATE USER crm WITH PASSWORD 'crm';
    END IF;
END
$$;

GRANT ALL PRIVILEGES ON DATABASE crm_read TO crm;
GRANT ALL ON SCHEMA public TO crm;
GRANT pg_create_subscription TO crm;

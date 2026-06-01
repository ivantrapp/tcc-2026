-- =================================================================
-- Setup da Replicação Lógica WAL — Microsserviços → crm_read
-- =================================================================
-- Cada microsserviço possui seu próprio banco de dados.
-- Cada banco publica suas tabelas; crm_read as assina individualmente.
--
-- PRÉ-REQUISITO: wal_level = logical em cada banco de origem.
-- O docker-compose já configura isso via -c wal_level=logical.
--
-- PASSO 1 — Execute em cada banco de origem como superusuário:
--   psql -U postgres -d crm_staff   -p 5457 -f setup-wal-replication.sql
--   psql -U postgres -d crm_customer -p 5458 -f setup-wal-replication.sql
--   psql -U postgres -d crm_wallet  -p 5459 -f setup-wal-replication.sql
--
-- PASSO 2 — Execute a seção de subscrições no crm_read:
--   psql -U postgres -d crm_read -p 5455
--
-- PASSO 3 (monolito) — Execute no crm_write se quiser a replicação
--   do write app monolítico:
--   psql -U postgres -d crm_write -p 5456 -f setup-wal-replication.sql
-- =================================================================


-- =================================================================
-- PARTE 1A — Staff DB (crm_staff, porta 5457)
-- Tabelas: branches, account_managers
-- =================================================================

DROP PUBLICATION IF EXISTS crm_staff_publication;
CREATE PUBLICATION crm_staff_publication
    FOR TABLE branches, account_managers
    WITH (publish = 'insert, update, delete');

GRANT SELECT ON branches, account_managers TO replicator;


-- =================================================================
-- PARTE 1B — Customer DB (crm_customer, porta 5458)
-- Tabelas: customers
-- =================================================================

DROP PUBLICATION IF EXISTS crm_customer_publication;
CREATE PUBLICATION crm_customer_publication
    FOR TABLE customers
    WITH (publish = 'insert, update, delete');

GRANT SELECT ON customers TO replicator;


-- =================================================================
-- PARTE 1C — Wallet DB (crm_wallet, porta 5459)
-- Tabelas: wallets, transactions
-- =================================================================

DROP PUBLICATION IF EXISTS crm_wallet_publication;
CREATE PUBLICATION crm_wallet_publication
    FOR TABLE wallets, transactions
    WITH (publish = 'insert, update, delete');

GRANT SELECT ON wallets, transactions TO replicator;


-- =================================================================
-- PARTE 1D — Write DB monolítico (crm_write, porta 5456)
-- Somente necessário se quiser replicar o app monolítico
-- =================================================================

DROP PUBLICATION IF EXISTS crm_publication;
CREATE PUBLICATION crm_publication
    FOR TABLE branches, account_managers, customers, wallets, transactions
    WITH (publish = 'insert, update, delete');

GRANT SELECT ON branches, account_managers, customers, wallets, transactions TO replicator;


-- =================================================================
-- PARTE 2 — Read DB (crm_read, porta 5455)
-- Execute conectado ao crm_read como superusuário.
--
-- Substitua os hosts pelos IPs/hostnames corretos se não usar Docker.
-- No Docker, use os service names: db-staff, db-customer, db-wallet.
-- Localmente sem Docker, use localhost com as portas mapeadas.
-- =================================================================

/*

-- Microsserviços: 3 subscriptions independentes

DROP SUBSCRIPTION IF EXISTS crm_staff_subscription;
CREATE SUBSCRIPTION crm_staff_subscription
    CONNECTION 'host=db-staff port=5432 dbname=crm_staff user=replicator password=replicator'
    PUBLICATION crm_staff_publication;

DROP SUBSCRIPTION IF EXISTS crm_customer_subscription;
CREATE SUBSCRIPTION crm_customer_subscription
    CONNECTION 'host=db-customer port=5432 dbname=crm_customer user=replicator password=replicator'
    PUBLICATION crm_customer_publication;

DROP SUBSCRIPTION IF EXISTS crm_wallet_subscription;
CREATE SUBSCRIPTION crm_wallet_subscription
    CONNECTION 'host=db-wallet port=5432 dbname=crm_wallet user=replicator password=replicator'
    PUBLICATION crm_wallet_publication;


-- Monolito (opcional, se quiser manter a replicação do write monolítico):

DROP SUBSCRIPTION IF EXISTS crm_subscription;
CREATE SUBSCRIPTION crm_subscription
    CONNECTION 'host=db-write port=5432 dbname=crm_write user=replicator password=replicator'
    PUBLICATION crm_publication;

*/

-- Após criar as subscriptions:
-- 1. O PostgreSQL copia automaticamente todos os dados existentes (initial sync)
-- 2. Em seguida inicia o streaming de novos eventos via WAL
-- 3. A latência em condições normais é de microssegundos

-- Para verificar o status (em cada banco de origem):
-- SELECT * FROM pg_replication_slots;
-- SELECT * FROM pg_stat_replication;

-- Para verificar no Read DB:
-- SELECT * FROM pg_stat_subscription;

-- =================================================================
-- Setup da Replicação Lógica WAL entre crm_write e crm_read
-- =================================================================
-- PRÉ-REQUISITO: wal_level = logical no postgresql.conf do Write DB.
-- Verifique com: SHOW wal_level;
-- Se não for 'logical', adicione ao postgresql.conf:
--   wal_level = logical
-- e reinicie o PostgreSQL.
--
-- PASSO 1 — Execute no Write DB (crm_write) como superusuário:
--   psql -U postgres -d crm_write -p 5432 -f setup-wal-replication.sql
--
-- PASSO 2 — Execute a seção de subscrição no Read DB (crm_read)
--   como superusuário separadamente (veja abaixo).
-- =================================================================


-- =================================================================
-- PARTE 1 — Write DB (crm_write)
-- =================================================================

-- Usuário de replicação (usado exclusivamente pela subscrição)
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'replicator') THEN
        CREATE USER replicator WITH REPLICATION PASSWORD 'replicator';
    END IF;
END
$$;

-- Permissão de leitura nas tabelas publicadas
GRANT SELECT ON branches, account_managers, customers, wallets, transactions TO replicator;

-- Publicação: declara quais tabelas participam da replicação
-- Captura INSERT e UPDATE (transações nunca são deletadas)
DROP PUBLICATION IF EXISTS crm_publication;
CREATE PUBLICATION crm_publication
    FOR TABLE branches, account_managers, customers, wallets, transactions
    WITH (publish = 'insert, update, delete');


-- =================================================================
-- PARTE 2 — Read DB (crm_read)
-- Execute este bloco conectado ao crm_read como superusuário:
--   psql -U postgres -d crm_read -p 5433
-- =================================================================

-- Substitua 'write-db-host' pelo IP/hostname do Write DB
-- Se ambos rodam no mesmo host (Docker local): use o IP do container
-- ou o hostname definido no docker-compose (ex: db-write)

/*
DROP SUBSCRIPTION IF EXISTS crm_subscription;

CREATE SUBSCRIPTION crm_subscription
    CONNECTION 'host=localhost port=5432 dbname=crm_write user=replicator password=replicator'
    PUBLICATION crm_publication;
*/

-- Após criar a subscrição:
-- 1. O PostgreSQL copia automaticamente todos os dados existentes (initial sync)
-- 2. Em seguida inicia o streaming de novos eventos via WAL
-- 3. A latência em condições normais é de microssegundos

-- Para verificar o status da replicação (no Write DB):
-- SELECT * FROM pg_replication_slots;
-- SELECT * FROM pg_stat_replication;

-- Para verificar no Read DB:
-- SELECT * FROM pg_stat_subscription;

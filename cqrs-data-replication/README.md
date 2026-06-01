# CQRS — Data Replication

Projeto de TCC demonstrando o padrão CQRS com replicação de dados via PostgreSQL WAL (Write-Ahead Log).

## Arquitetura

```
Write side                          Read side
──────────────────────────────────  ─────────────────────
write-app  (8080) → db-write (5456)
staff-app  (8082) → db-staff (5457) ──WAL──┐
customer-app (8083) → db-customer (5458) ──WAL──┼──► db-read (5455) → read-app (8081)
wallet-app (8084) → db-wallet (5459) ──WAL──┘
```

Validações cross-service (ex: customer verifica se account manager existe) são feitas via HTTP na **read-app** — nunca diretamente em outro microsserviço de escrita.

## Pré-requisitos

- Docker e Docker Compose v2
- Portas livres: `8080–8084`, `8081`, `5455–5459`

## Subir o ambiente

Execute sempre com `--build --force-recreate` para garantir que imagens e containers sejam recriados do zero:

```bash
docker compose up --build --force-recreate -d
```

Acompanhe os logs até todas as apps estarem prontas (mensagem `Started Application`):

```bash
docker compose logs -f
```

Quando as 5 apps subirem, siga para o setup da replicação.

## Setup da replicação WAL

> Este passo é **manual** e deve ser executado **uma única vez** após as apps iniciarem (o Flyway precisa criar as tabelas antes de publicá-las).

### 1 — Criar as publicações nos bancos de origem

```bash
# Staff (branches, account_managers)
docker compose exec -T db-staff psql -U postgres -d crm_staff -c "
  DROP PUBLICATION IF EXISTS crm_staff_publication;
  CREATE PUBLICATION crm_staff_publication
    FOR TABLE branches, account_managers
    WITH (publish = 'insert, update, delete');
  GRANT SELECT ON branches, account_managers TO replicator;
"

# Customer (customers)
docker compose exec -T db-customer psql -U postgres -d crm_customer -c "
  DROP PUBLICATION IF EXISTS crm_customer_publication;
  CREATE PUBLICATION crm_customer_publication
    FOR TABLE customers
    WITH (publish = 'insert, update, delete');
  GRANT SELECT ON customers TO replicator;
"

# Wallet (wallets, transactions)
docker compose exec -T db-wallet psql -U postgres -d crm_wallet -c "
  DROP PUBLICATION IF EXISTS crm_wallet_publication;
  CREATE PUBLICATION crm_wallet_publication
    FOR TABLE wallets, transactions
    WITH (publish = 'insert, update, delete');
  GRANT SELECT ON wallets, transactions TO replicator;
"
```

### 2 — Criar as subscriptions no banco de leitura

```bash
docker compose exec -T db-read psql -U postgres -d crm_read -c "
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
"
```

> **Atalho:** rode `./setup-wal.sh` (Linux/Mac/Git Bash) para executar os dois passos de uma vez.

### Replicação do monolito (opcional)

Se quiser também replicar o write-app monolítico:

```bash
docker compose exec -T db-write psql -U postgres -d crm_write -c "
  DROP PUBLICATION IF EXISTS crm_publication;
  CREATE PUBLICATION crm_publication
    FOR TABLE branches, account_managers, customers, wallets, transactions
    WITH (publish = 'insert, update, delete');
  GRANT SELECT ON branches, account_managers, customers, wallets, transactions TO replicator;
"

docker compose exec -T db-read psql -U postgres -d crm_read -c "
  DROP SUBSCRIPTION IF EXISTS crm_subscription;
  CREATE SUBSCRIPTION crm_subscription
    CONNECTION 'host=db-write port=5432 dbname=crm_write user=replicator password=replicator'
    PUBLICATION crm_publication;
"
```

## Verificar a replicação

```bash
# Status dos replication slots em cada banco de origem
docker compose exec -T db-staff    psql -U postgres -d crm_staff    -c "SELECT slot_name, active FROM pg_replication_slots;"
docker compose exec -T db-customer psql -U postgres -d crm_customer -c "SELECT slot_name, active FROM pg_replication_slots;"
docker compose exec -T db-wallet   psql -U postgres -d crm_wallet   -c "SELECT slot_name, active FROM pg_replication_slots;"

# Status das subscriptions no banco de leitura
docker compose exec -T db-read psql -U postgres -d crm_read -c "SELECT subname, subenabled FROM pg_stat_subscription;"
```

Todos os campos `active` / `subenabled` devem ser `t` (true).

## Parar o ambiente

Apenas para os containers (dados preservados):

```bash
docker compose down
```

Remove também os volumes (banco zerado na próxima subida):

```bash
docker compose down -v
```

## Portas

| Serviço          | Porta  | Banco      | Porta DB |
|------------------|--------|------------|----------|
| write-app        | 8080   | crm_write  | 5456     |
| read-app         | 8081   | crm_read   | 5455     |
| staff-app        | 8082   | crm_staff  | 5457     |
| customer-app     | 8083   | crm_customer | 5458   |
| wallet-app       | 8084   | crm_wallet | 5459     |

## Credenciais

| Usuário      | Senha       | Papel                        |
|--------------|-------------|------------------------------|
| `postgres`   | `postgres`  | superusuário (Docker only)   |
| `crm`        | `crm`       | app (leitura + escrita)      |
| `replicator` | `replicator`| replicação lógica WAL        |

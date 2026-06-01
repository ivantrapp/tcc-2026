#!/usr/bin/env bash
set -euo pipefail

echo "==> [1/4] Publicação: crm_staff"
docker compose exec -T db-staff psql -U postgres -d crm_staff -c "
  DROP PUBLICATION IF EXISTS crm_staff_publication;
  CREATE PUBLICATION crm_staff_publication
    FOR TABLE branches, account_managers
    WITH (publish = 'insert, update, delete');
  GRANT SELECT ON branches, account_managers TO replicator;
"

echo "==> [2/4] Publicação: crm_customer"
docker compose exec -T db-customer psql -U postgres -d crm_customer -c "
  DROP PUBLICATION IF EXISTS crm_customer_publication;
  CREATE PUBLICATION crm_customer_publication
    FOR TABLE customers
    WITH (publish = 'insert, update, delete');
  GRANT SELECT ON customers TO replicator;
"

echo "==> [3/4] Publicação: crm_wallet"
docker compose exec -T db-wallet psql -U postgres -d crm_wallet -c "
  DROP PUBLICATION IF EXISTS crm_wallet_publication;
  CREATE PUBLICATION crm_wallet_publication
    FOR TABLE wallets, transactions
    WITH (publish = 'insert, update, delete');
  GRANT SELECT ON wallets, transactions TO replicator;
"

echo "==> [4/4] Subscriptions: crm_read"
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

echo ""
echo "Replicacao WAL configurada. Verificando subscriptions..."
docker compose exec -T db-read psql -U postgres -d crm_read -c \
  "SELECT subname, subenabled FROM pg_stat_subscription;"

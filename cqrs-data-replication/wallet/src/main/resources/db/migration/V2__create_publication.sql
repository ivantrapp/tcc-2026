DROP PUBLICATION IF EXISTS crm_wallet_publication;
CREATE PUBLICATION crm_wallet_publication
    FOR TABLE wallets, transactions
    WITH (publish = 'insert, update, delete');

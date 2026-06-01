DROP PUBLICATION IF EXISTS crm_customer_publication;
CREATE PUBLICATION crm_customer_publication
    FOR TABLE customers
    WITH (publish = 'insert, update, delete');

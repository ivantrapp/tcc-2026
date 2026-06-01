DROP PUBLICATION IF EXISTS crm_staff_publication;
CREATE PUBLICATION crm_staff_publication
    FOR TABLE branches, account_managers
    WITH (publish = 'insert, update, delete');

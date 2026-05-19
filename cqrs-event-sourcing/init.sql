-- Script de init do container Docker (roda como superuser "crm" automaticamente).
-- O banco crm_write já é criado pelo POSTGRES_DB — aqui só criamos o crm_read.
CREATE DATABASE crm_read;

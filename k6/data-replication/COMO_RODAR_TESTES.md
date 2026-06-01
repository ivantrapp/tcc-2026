# Como rodar os testes k6 — Data Replication

## Pré-requisitos

- k6 instalado (`choco install k6` ou https://k6.io/docs/get-started/installation/)
- Os serviços rodando via docker compose (ver `cqrs-data-replication/README.md`)
- Replicação WAL ativa (configurada automaticamente pelo `SubscriptionConfig` ao subir)

## Rodar os testes

> No PowerShell use backtick `` ` `` para quebrar linha. Não use `\` (sintaxe bash).

### 01 — Write Load (throughput de escrita nos microsserviços)

```powershell
k6 run -e STAFF_URL=http://localhost:8082 `
       -e CUSTOMER_URL=http://localhost:8083 `
       -e WALLET_URL=http://localhost:8084 `
       01-write-load.js
```

### 02 — Read Load (throughput de leitura via read app)

```powershell
k6 run -e STAFF_URL=http://localhost:8082 `
       -e CUSTOMER_URL=http://localhost:8083 `
       -e WALLET_URL=http://localhost:8084 `
       -e READ_URL=http://localhost:8081 `
       02-read-load.js
```

### 03 — Replication Lag (latência WAL: escrita → visibilidade no read DB)

```powershell
k6 run -e STAFF_URL=http://localhost:8082 `
       -e CUSTOMER_URL=http://localhost:8083 `
       -e WALLET_URL=http://localhost:8084 `
       -e READ_URL=http://localhost:8081 `
       03-lag.js
```

Número de iterações customizável (padrão: 100):

```powershell
k6 run -e STAFF_URL=http://localhost:8082 `
       -e CUSTOMER_URL=http://localhost:8083 `
       -e WALLET_URL=http://localhost:8084 `
       -e READ_URL=http://localhost:8081 `
       -e ITERATIONS=50 `
       03-lag.js
```

## Modo monolito (write app na porta 8080)

Passe os três URLs de escrita apontando para o write-app monolítico:

```powershell
k6 run -e STAFF_URL=http://localhost:8080 `
       -e CUSTOMER_URL=http://localhost:8080 `
       -e WALLET_URL=http://localhost:8080 `
       -e READ_URL=http://localhost:8081 `
       01-write-load.js
```

## O que cada teste mede

| Teste | O que mede | Threshold |
|---|---|---|
| `01-write-load.js` | Throughput e latência dos microsserviços de escrita sob carga de transações | p(95) < 1000ms, erros < 5% |
| `02-read-load.js` | Throughput e latência da read app sob carga de consultas | p(95) < 500ms, erros < 5% |
| `03-lag.js` | Tempo entre escrita no wallet-app e dado visível na read-app via WAL | p(95) < 2000ms, timeouts < 3 |

## Fluxo do setup (executado automaticamente antes do teste)

Todos os testes criam os dados necessários automaticamente na função `setup()`:

```
POST /branches              →  staff-app  (8082)
POST /account-managers      →  staff-app  (8082)   usa branchId acima
POST /customers             →  customer-app (8083)  usa accountManagerId acima
POST /wallets               →  wallet-app  (8084)   usa customerId acima
POST /wallets/{id}/transactions  →  wallet-app (8084)
```

Não é necessário criar dados manualmente antes de rodar.

## Salvar resultado em JSON

```powershell
k6 run --out json=resultado-write-replication.json `
       -e STAFF_URL=http://localhost:8082 `
       -e CUSTOMER_URL=http://localhost:8083 `
       -e WALLET_URL=http://localhost:8084 `
       01-write-load.js

k6 run --out json=resultado-lag-replication.json `
       -e STAFF_URL=http://localhost:8082 `
       -e CUSTOMER_URL=http://localhost:8083 `
       -e WALLET_URL=http://localhost:8084 `
       -e READ_URL=http://localhost:8081 `
       03-lag.js
```

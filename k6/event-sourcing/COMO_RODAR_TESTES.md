# Como rodar os testes k6 — Event Sourcing

## Pré-requisitos

- k6 instalado (`choco install k6` ou https://k6.io/docs/get-started/installation/)
- Os serviços rodando via docker compose (ver `cqrs-event-sourcing/COMO_RODAR.md`)

## Rodar os testes

> No PowerShell use backtick `` ` `` para quebrar linha. Não use `\` (sintaxe bash).

### 01 — Write Load (throughput de escrita)

```powershell
k6 run -e STAFF_URL=http://localhost:8082 `
       -e CUSTOMER_URL=http://localhost:8083 `
       -e WALLET_URL=http://localhost:8084 `
       01-write-load.js
```

### 02 — Read Load (throughput de leitura)

```powershell
k6 run -e STAFF_URL=http://localhost:8082 `
       -e CUSTOMER_URL=http://localhost:8083 `
       -e WALLET_URL=http://localhost:8084 `
       -e READ_URL=http://localhost:8081 `
       02-read-load.js
```

### 03 — Projection Lag (latência de projeção RabbitMQ → read DB)

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

### 04 — Method Benchmark (média aritmética por método — 1000 iterações)

```powershell
k6 run -e STAFF_URL=http://localhost:8082 `
       -e CUSTOMER_URL=http://localhost:8083 `
       -e WALLET_URL=http://localhost:8084 `
       -e READ_URL=http://localhost:8081 `
       04-method-benchmark.js
```

Número de iterações customizável (padrão: 1000):

```powershell
k6 run -e STAFF_URL=http://localhost:8082 `
       -e CUSTOMER_URL=http://localhost:8083 `
       -e WALLET_URL=http://localhost:8084 `
       -e READ_URL=http://localhost:8081 `
       -e ITERATIONS=500 `
       04-method-benchmark.js
```

## O que cada teste mede

| Teste | O que mede | Threshold |
|---|---|---|
| `01-write-load.js` | Throughput e latência dos microsserviços de escrita sob carga | p(95) < 1000ms, erros < 5% |
| `02-read-load.js` | Throughput e latência da read app sob carga de consultas | p(95) < 500ms, erros < 5% |
| `03-lag.js` | Tempo entre escrita no wallet service e projeção visível na read app | p(95) < 2000ms, timeouts < 3 |
| `04-method-benchmark.js` | Média aritmética do tempo de execução de cada método (1 VU, 1000 iterações) | — |

## Fluxo do setup (executado automaticamente antes do teste)

Todos os testes criam os dados necessários automaticamente na função `setup()`:

```
POST /account-managers  →  staff (8082)
POST /customers         →  customer (8083)   usa accountManagerId acima
POST /wallets           →  wallet (8084)      usa customerId acima
POST /wallets/{id}/transactions  →  wallet (8084)
```

Não é necessário criar dados manualmente antes de rodar.

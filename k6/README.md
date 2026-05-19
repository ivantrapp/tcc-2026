# Testes de Performance — k6

Comparação de throughput, latência e lag de consistência entre os dois projetos CQRS.

## Instalação do k6

```powershell
# Windows (via winget)
winget install k6 --source winget

# Ou via Chocolatey
choco install k6

# Ou via Docker (sem instalação)
docker run --rm -i grafana/k6 run - < script.js
```

## Estrutura

```
k6/
  data-replication/
    01-write-load.js   # throughput de escrita (transações)
    02-read-load.js    # throughput de leitura (queries)
    03-lag.js          # lag de replicação WAL
  event-sourcing/
    01-write-load.js   # throughput de escrita (transações)
    02-read-load.js    # throughput de leitura (queries)
    03-lag.js          # lag de projeção (RabbitMQ)
```

## Como rodar

### 1. Suba o projeto que deseja testar

```powershell
# Data Replication
cd cqrs-data-replication
docker compose up --build

# Event Sourcing
cd cqrs-event-sourcing
docker compose up --build
```

### 2. Execute os scripts

```powershell
# Dentro da pasta k6/

# -- Data Replication --
k6 run data-replication/01-write-load.js
k6 run data-replication/02-read-load.js
k6 run data-replication/03-lag.js

# -- Event Sourcing --
k6 run event-sourcing/01-write-load.js
k6 run event-sourcing/02-read-load.js
k6 run event-sourcing/03-lag.js
```

### Portas configuráveis via variáveis de ambiente

```powershell
k6 run -e WRITE_URL=http://localhost:8080 -e READ_URL=http://localhost:8081 data-replication/01-write-load.js
```

## Cenários

| Script | VUs | Duração | O que mede |
|--------|-----|---------|------------|
| 01-write-load | 20 | 60s (ramp) | requests/s e latência de POST /wallets/{id}/transactions |
| 02-read-load  | 50 | 60s (ramp) | requests/s e latência de GET /customers/** |
| 03-lag        |  1 | 30 iter   | tempo (ms) entre escrita e visibilidade no read |

## Métricas principais no output do k6

```
http_req_duration............: avg=XX   min=XX   med=XX   max=XX   p(90)=XX  p(95)=XX
http_req_failed..............: XX%
http_reqs....................: XX  XX/s
replication_lag_ms (ou projection_lag_ms):
                               avg=XX   min=XX   med=XX   max=XX   p(90)=XX  p(95)=XX
```

## Salvando resultado em JSON (para análise posterior)

```powershell
k6 run --out json=resultado-write-replication.json data-replication/01-write-load.js
k6 run --out json=resultado-write-eventsourcing.json event-sourcing/01-write-load.js
```

## Parâmetros configuráveis

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| WRITE_URL | http://localhost:8080 | URL do write app |
| READ_URL  | http://localhost:8081 | URL do read app |
| ITERATIONS | 30 | Número de medições de lag (apenas 03-lag.js) |

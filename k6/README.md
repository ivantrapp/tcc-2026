# Testes de Performance — k6

Comparação de throughput, latência e lag de consistência entre os dois projetos CQRS.

## Instalação do k6

```powershell
# Windows (via winget)
winget install k6 --source winget

# Ou via Chocolatey
choco install k6
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
  grafana/
    provisioning/      # datasource e dashboard provisionados automaticamente
  docker-compose.observability.yml
```

---

## Observabilidade (Grafana + InfluxDB)

O dashboard do Grafana é provisionado automaticamente e exibe em tempo real:

- **Virtual Users** — escada de carga: 100 → 200 → 300 VUs
- **Throughput** — requisições por segundo
- **Latência** — p90, p95, p99 e média (ms)
- **Taxa de Erros** — % de requisições com falha

### 1. Suba o stack de observabilidade

```powershell
# Dentro da pasta k6/
docker compose -f docker-compose.observability.yml up -d
```

Grafana disponível em: **http://localhost:3000**
O dashboard "K6 Load Test — CQRS" aparece automaticamente no menu lateral.

### 2. Rode o teste com saída para o InfluxDB

```powershell
# Data Replication
k6 run --out influxdb=http://localhost:8086/k6 data-replication/01-write-load.js

# Event Sourcing
k6 run --out influxdb=http://localhost:8086/k6 event-sourcing/01-write-load.js
```

Os dados aparecem no Grafana em tempo real com atualização a cada 5 segundos.

### 3. Derrube o stack ao terminar

```powershell
docker compose -f docker-compose.observability.yml down
```

---

## Rodar os testes sem Grafana

### 1. Suba o projeto que deseja testar

```powershell
# Data Replication
cd ../cqrs-data-replication
docker compose up --build

# Event Sourcing
cd ../cqrs-event-sourcing
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

Os microsserviços de escrita expõem três serviços separados. As URLs padrão são:

| Variável | Padrão | Serviço |
|----------|--------|---------|
| `STAFF_URL` | http://localhost:8082 | staff-service |
| `CUSTOMER_URL` | http://localhost:8083 | customer-service |
| `WALLET_URL` | http://localhost:8084 | wallet-service |

```powershell
k6 run \
  -e STAFF_URL=http://localhost:8082 \
  -e CUSTOMER_URL=http://localhost:8083 \
  -e WALLET_URL=http://localhost:8084 \
  event-sourcing/01-write-load.js
```

---

## Cenários de carga (01-write-load)

O teste de escrita sobe a carga em três patamares com 60 s de estabilização em cada nível:

| Fase | Duração | VUs |
|------|---------|-----|
| Ramp-up | 30 s | 0 → 100 |
| Patamar 1 | 60 s | 100 |
| Ramp-up | 30 s | 100 → 200 |
| Patamar 2 | 60 s | 200 |
| Ramp-up | 30 s | 200 → 300 |
| Patamar 3 | 60 s | 300 |
| Ramp-down | 30 s | 300 → 0 |

**Total: ~5 min**

## Cenários resumidos (todos os scripts)

| Script | VUs | Duração | O que mede |
|--------|-----|---------|------------|
| 01-write-load | 100 / 200 / 300 | ~5 min | throughput e latência de POST /transactions |
| 02-read-load | ramping | ~3 min | throughput e latência de GET /customers |
| 03-lag | 1 | 30 iter | tempo (ms) entre escrita e leitura consistente |

---

## Métricas principais no output do k6

```
http_req_duration............: avg=XX   p(90)=XX   p(95)=XX   p(99)=XX
http_req_failed..............: XX%
http_reqs....................: XX req/s
```

## Salvando resultado em JSON

```powershell
k6 run --out json=resultado-write-replication.json data-replication/01-write-load.js
k6 run --out json=resultado-write-eventsourcing.json event-sourcing/01-write-load.js
```

# TCC — Comparação CQRS: Event Sourcing vs Replicação de Dados

> Rode **um projeto por vez** — ambos usam as mesmas portas (8081–8084).

---

## Subindo os projetos

```powershell
# Data Replication
docker compose -f docker-compose.data-replication.yml down -v
docker compose -f docker-compose.data-replication.yml up --build --force-recreate -d

# Event Sourcing
docker compose -f docker-compose.event-sourcing.yml down -v
docker compose -f docker-compose.event-sourcing.yml up --build --force-recreate -d
```

---

## Observabilidade — Grafana + InfluxDB

```powershell
# Sobe (da raiz do projeto)
docker compose -f k6\docker-compose.observability.yml up -d

# Derruba ao terminar
docker compose -f k6\docker-compose.observability.yml down
```

Grafana disponível em **http://localhost:3000** — dashboard "K6 Load Test — CQRS" já provisionado automaticamente.

---

## Testes de performance (k6)

### Instalação (binário oficial — inclui web dashboard)

```powershell
Invoke-WebRequest -Uri "https://github.com/grafana/k6/releases/download/v0.54.0/k6-v0.54.0-windows-amd64.zip" -OutFile "k6-official.zip"
Expand-Archive -Path "k6-official.zip" -DestinationPath "k6-official"
$K6 = ".\k6-official\k6-v0.54.0-windows-amd64\k6.exe"
```

### Executar com saída para o Grafana

```powershell
# Data Replication
& $K6 run --out influxdb=http://localhost:8086/k6 k6\data-replication\01-write-load.js
& $K6 run --out influxdb=http://localhost:8086/k6 k6\data-replication\02-read-load.js
& $K6 run --out influxdb=http://localhost:8086/k6 k6\data-replication\03-lag.js

# Event Sourcing
& $K6 run --out influxdb=http://localhost:8086/k6 k6\event-sourcing\01-write-load.js
& $K6 run --out influxdb=http://localhost:8086/k6 k6\event-sourcing\02-read-load.js
& $K6 run --out influxdb=http://localhost:8086/k6 k6\event-sourcing\03-lag.js
```

### Scripts disponíveis

| Script | VUs (pico) | Duração | O que mede |
|--------|-----------|---------|------------|
| `01-write-load.js` | 300 | ~5 min | throughput e latência de escrita |
| `02-read-load.js` | 300 | ~3 min | throughput e latência de leitura |
| `03-lag.js` | 1 | ~100 iter | lag de consistência eventual (ms) |
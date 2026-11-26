# TCC — Comparação CQRS: Event Sourcing vs Replicação de Dados

> Rode **um projeto por vez** — ambos usam as mesmas portas (8081–8084).

---

## Subindo os projetos

```powershell
# Data Replication
docker compose -f docker-compose.data-replication.yml down -v
`docker compose -f docker-compose.data-replication.yml up --build --force-recreate -d
`
# Event Sourcing
docker compose -f docker-compose.event-sourcing.yml down -v
docker compose -f docker-compose.event-sourcing.yml up --build --force-recreate -d
```

---

## Observabilidade — Grafana + InfluxDB

```powershell
# Sobe (da raiz do projeto)
`docker compose -f k6\docker-compose.observability.yml up -d
docker compose -f k6\docker-compose.observability.yml up -d --force-recreate influxdb
`
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

### k6 em contêiner Linux (padrão dos scripts `run-*.ps1`)

Os scripts `run-*.ps1` rodam o k6 como contêiner (`grafana/k6:1.7.1`, mesma versão do `k6.exe`) **dentro da rede Docker do compose** testado, acessando os serviços pelo nome em vez de `localhost`:

| Projeto | Rede | URLs |
|---|---|---|
| Data Replication | `cqrs-data-replication_crm-network` | `staff-app:8082`, `customer-app:8083`, `wallet-app:8084`, `read-app:8081` |
| Event Sourcing | `cqrs-event-sourcing_default` | `staff:8082`, `customer:8083`, `wallet:8084`, `read:8081` |

Motivos: o Linux lida melhor com milhares de conexões; elimina o encaminhamento de portas do Docker Desktop (Windows → WSL2), que também consome conexões; e o gerador de carga fica isolado em seu próprio contêiner (Jiang e Hassan, 2015, seção 4.1.2). O contêiner sobe com `nofile=1048576`, `ip_local_port_range=1024 65535` e `tcp_tw_reuse=1`. As métricas vão para o InfluxDB via `host.docker.internal:8086`. A configuração fica em `k6/k6-docker.ps1`.

> Observação: no Docker Desktop o contêiner do k6 roda na mesma VM WSL2 das aplicações, portanto divide CPU/RAM com o sistema sob teste (assim como o `k6.exe` dividia com o host). O isolamento é de processo/rede, não de hardware.

Para usar o modo antigo (`k6.exe` no Windows + `localhost`), passe `-LocalK6` a qualquer script. Execução avulsa equivalente:

```powershell
docker run --rm --network cqrs-data-replication_crm-network `
  -v "${PWD}\k6\data-replication:/scripts:ro" grafana/k6:1.7.1 run `
  -e STAFF_URL=http://staff-app:8082 -e CUSTOMER_URL=http://customer-app:8083 `
  -e WALLET_URL=http://wallet-app:8084 -e READ_URL=http://read-app:8081 `
  --out influxdb=http://host.docker.internal:8086/k6 /scripts/02-read-load-10000.js
```

### Bateria de carga crescente (Data Replication — write)

Roda todos os testes de write em sequência (300 → 5000 VUs), valida os 4 serviços antes de cada teste e salva o resultado de cada execução em `k6/data-replication/results/`.

```powershell
# Modo padrão — envia métricas ao InfluxDB/Grafana
.\run-write-load-tests.ps1

# Cooldown maior entre testes (padrão: 60s)
.\run-write-load-tests.ps1 -CooldownSeconds 90

# Sem InfluxDB (não exige o container, omite o --out do k6)
.\run-write-load-tests.ps1 -SkipInfluxDB
```

O script valida `staff-app:8082`, `customer-app:8083`, `wallet-app:8084` e `read-app:8081` (+ `influxdb:8086`) antes de cada rodada. Se algum serviço estiver fora, aguarda até 5 tentativas de 15 s antes de abortar.

### Bateria de carga crescente (Data Replication — read)

Roda todos os testes de leitura em sequência (300 → 14000 VUs), valida os 4 serviços antes de cada teste e salva o resultado de cada execução em `k6/data-replication/results/` (arquivos `*-read-<VUs>vus.log`).

```powershell
# Modo padrão — envia métricas ao InfluxDB/Grafana
.\run-read-load-tests.ps1

# Cooldown maior entre testes (padrão: 60s)
.\run-read-load-tests.ps1 -CooldownSeconds 90

# Sem InfluxDB (não exige o container, omite o --out do k6)
.\run-read-load-tests.ps1 -SkipInfluxDB
```

O script valida os mesmos serviços da bateria de write antes de cada rodada, com a mesma lógica de retentativa.

### Scripts disponíveis

| Script | VUs (pico) | Duração aprox. | O que mede |
|--------|------------|----------------|------------|
| `01-write-load.js` | 300 | ~5 min | throughput e latência de escrita |
| `01-write-load-500.js` | 500 | ~7 min | throughput e latência de escrita |
| `01-write-load-1000.js` | 1000 | ~7 min | throughput e latência de escrita |
| `01-write-load-2000.js` | 2000 | ~7 min | throughput e latência de escrita |
| `01-write-load-3000.js` | 3000 | ~7 min | throughput e latência de escrita |
| `01-write-load-4000.js` | 4000 | ~7 min | throughput e latência de escrita |
| `01-write-load-5000.js` | 5000 | ~7 min | throughput e latência de escrita |
| `02-read-load.js` | 300 | ~3 min | throughput e latência de leitura |
| `03-lag.js` | 1 | ~100 iter | lag de consistência eventual (ms) |
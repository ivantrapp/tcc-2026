# TCC — Comparação CQRS: Event Sourcing vs Replicação de Dados

## Projetos

| Projeto | Write | Read | Mecanismo |
|---------|-------|------|-----------|
| `cqrs-data-replication` | :8080 | :8081 | PostgreSQL WAL Logical Replication |
| `cqrs-event-sourcing`   | :8080 | :8081 | RabbitMQ + Event Store |

---

## Subindo os projetos

> Rode apenas **um projeto por vez** — ambos usam as mesmas portas.

```powershell
# Data Replication
cd cqrs-data-replication
docker compose down -v   # limpa volumes anteriores se necessário
docker compose up --build

# Event Sourcing
cd cqrs-event-sourcing
docker compose down -v
docker compose up --build
```

---

## Testes de performance (k6)

### Pré-requisito — k6 v0.54 (sem web dashboard no Chocolatey)

O pacote do Chocolatey não inclui o web dashboard. Use o binário oficial:

```powershell
# Na pasta raiz do projeto
Invoke-WebRequest -Uri "https://github.com/grafana/k6/releases/download/v0.54.0/k6-v0.54.0-windows-amd64.zip" -OutFile "k6-official.zip"
Expand-Archive -Path "k6-official.zip" -DestinationPath "k6-official"
```

O executável fica em `k6-official\k6-v0.54.0-windows-amd64\k6.exe`.

---

### Rodar um teste com visualização em tempo real

```powershell
$env:K6_WEB_DASHBOARD = "true"
.\k6-official\k6-v0.54.0-windows-amd64\k6.exe run k6\data-replication\01-write-load.js
```

Abra **http://localhost:5665** no browser — o dashboard abre em tempo real.

> O `setup()` cria os dados de teste antes de iniciar a carga. Aguarde terminar.

---

### Scripts disponíveis

| Script | VUs (pico) | Duração | O que mede |
|--------|-----------|---------|------------|
| `01-write-load.js` | 200 | ~135s | Throughput e latência de escrita (POST /wallets/{id}/transactions) |
| `02-read-load.js`  | 500 | ~135s | Throughput e latência de leitura (GET /customers/**) |
| `03-lag.js`        |   1 | ~100 iter | Lag de consistência eventual (ms entre escrita e visibilidade no read) |

---

### Exemplos de execução

```powershell
# Executável do k6 oficial
$K6 = ".\k6-official\k6-v0.54.0-windows-amd64\k6.exe"

# Habilitar web dashboard
$env:K6_WEB_DASHBOARD = "true"

# --- Data Replication ---
& $K6 run k6\data-replication\01-write-load.js
& $K6 run k6\data-replication\02-read-load.js
& $K6 run k6\data-replication\03-lag.js

# --- Event Sourcing ---
& $K6 run k6\event-sourcing\01-write-load.js
& $K6 run k6\event-sourcing\02-read-load.js
& $K6 run k6\event-sourcing\03-lag.js
```

### Exportar relatório HTML

```powershell
$env:K6_WEB_DASHBOARD = "true"
$env:K6_WEB_DASHBOARD_EXPORT = "relatorio-write-replication.html"
.\k6-official\k6-v0.54.0-windows-amd64\k6.exe run k6\data-replication\01-write-load.js
```

O arquivo HTML gerado pode ser aberto no browser sem nenhuma dependência.

---

### Alterar número de iterações do teste de lag

```powershell
$env:K6_WEB_DASHBOARD = "true"
.\k6-official\k6-v0.54.0-windows-amd64\k6.exe run -e ITERATIONS=50 k6\data-replication\03-lag.js
```

---

## Coleções Postman

| Arquivo | Projeto |
|---------|---------|
| `cqrs-data-replication\CQRS-Data-Replication.postman_collection.json` | Data Replication |
| `cqrs-event-sourcing\CQRS-Event-Sourcing.postman_collection.json` | Event Sourcing |

Importe no Postman e execute na ordem — cada request de criação já salva o ID gerado nas variáveis da collection.

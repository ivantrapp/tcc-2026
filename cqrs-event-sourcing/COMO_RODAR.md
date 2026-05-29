# Como rodar os microsserviços com Docker Compose

## Pré-requisitos

- Docker Desktop em execução
- Portas livres: 5440, 5672, 15672, 8081, 8082, 8083, 8084

## Subir apenas os microsserviços de escrita + read

```bash
docker compose up --build staff customer wallet read
```

## Reset completo + rebuild (banco zerado)

```bash
docker compose down -v && docker compose up --build staff customer wallet read
```

Isso sobe:
- `postgres` e `rabbitmq` automaticamente (são dependências)
- `read` (8081) — leitura / projeções
- `staff` (8082) — escrita AccountManager
- `customer` (8083) — escrita Customer
- `wallet` (8084) — escrita Wallet

> `write` (monolito de referência, porta 8080) **não** sobe com esse comando.

## Ordem de inicialização

```
postgres + rabbitmq
        ↓
       read  ←── staff
        ↓
  customer + wallet
```

`customer` e `wallet` aguardam a `read` iniciar porque fazem validações cross-domain consultando a read app.

## Verificar se os serviços estão de pé

```bash
# read
curl http://localhost:8081/account-managers
curl http://localhost:8081/customers

# staff
curl http://localhost:8082/account-managers

# customer
curl http://localhost:8083/customers

# wallet
curl http://localhost:8084/wallets
```

## Fluxo básico de teste

### 1. Criar um AccountManager (staff)
```bash
curl -X POST http://localhost:8082/account-managers \
  -H "Content-Type: application/json" \
  -d '{"name": "Ana Silva"}'
```

### 2. Criar um Customer (customer)
> Use o `accountManagerId` retornado no passo anterior se quiser associar.
```bash
curl -X POST http://localhost:8083/customers \
  -H "Content-Type: application/json" \
  -d '{"name": "João Costa", "email": "joao@email.com"}'
```

### 3. Criar uma Wallet (wallet)
> Use o `customerId` retornado no passo anterior.
```bash
curl -X POST http://localhost:8084/wallets \
  -H "Content-Type: application/json" \
  -d '{"customerId": "<uuid-do-customer>"}'
```

### 4. Consultar o estado na read app
```bash
curl http://localhost:8081/customers
curl http://localhost:8081/account-managers
```

> A read app recebe os eventos via RabbitMQ (`crm.events`) e projeta nas views. Pode haver um delay de milissegundos entre a escrita e a leitura aparecer.

## Parar tudo

```bash
docker compose down
```

Para apagar também o volume do banco (reset completo):

```bash
docker compose down -v
```

## RabbitMQ Management UI

Acesse `http://localhost:15672` com usuário `guest` / senha `guest` para inspecionar filas e mensagens.

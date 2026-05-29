/**
 * CQRS Event Sourcing — Method Benchmark
 * Cada método básico do sistema é chamado 1000 vezes em sequência.
 * A média aritmética do tempo de execução é calculada ao final.
 *
 * Metodologia: 1 VU, sem concorrência — mede latência individual de cada operação.
 *
 * Uso:
 *   k6 run 04-method-benchmark.js
 *   k6 run -e ITERATIONS=1000 -e STAFF_URL=http://localhost:8082 -e CUSTOMER_URL=http://localhost:8083 -e WALLET_URL=http://localhost:8084 -e READ_URL=http://localhost:8081 04-method-benchmark.js
 */

import http from 'k6/http';
import { sleep } from 'k6';
import { Trend } from 'k6/metrics';

const STAFF_URL    = __ENV.STAFF_URL    || 'http://localhost:8082';
const CUSTOMER_URL = __ENV.CUSTOMER_URL || 'http://localhost:8083';
const WALLET_URL   = __ENV.WALLET_URL   || 'http://localhost:8084';
const READ_URL     = __ENV.READ_URL     || 'http://localhost:8081';
const ITERATIONS   = parseInt(__ENV.ITERATIONS || '1000');
const HEADERS      = { 'Content-Type': 'application/json' };

const FIXED_BRANCH_ID = '00000000-0000-0000-0000-000000000001';

const tCreateAccountManager = new Trend('exec_create_account_manager_ms', true);
const tCreateCustomer       = new Trend('exec_create_customer_ms',        true);
const tCreateWallet         = new Trend('exec_create_wallet_ms',          true);
const tRegisterTransaction  = new Trend('exec_register_transaction_ms',   true);
const tReadAccountManager   = new Trend('exec_read_account_manager_ms',   true);
const tReadCustomer         = new Trend('exec_read_customer_ms',          true);
const tListCustomers        = new Trend('exec_list_customers_ms',         true);

export const options = {
  scenarios: {
    benchmark: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
    },
  },
};

function randomUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = Math.random() * 16 | 0;
    return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
  });
}

export function setup() {
  const amRes = http.post(`${STAFF_URL}/account-managers`,
    JSON.stringify({ name: 'Gerente Benchmark', branchId: FIXED_BRANCH_ID, isAdmin: false }),
    { headers: HEADERS });
  const accountManagerId = amRes.json('accountManagerId');
  sleep(1);

  const custRes = http.post(`${CUSTOMER_URL}/customers`,
    JSON.stringify({ name: 'Cliente Benchmark', address: 'Rua Benchmark, 1', age: 30, accountManagerId }),
    { headers: HEADERS });
  const customerId = custRes.json('customerId');
  sleep(1);

  const walletRes = http.post(`${WALLET_URL}/wallets`,
    JSON.stringify({ customerId }), { headers: HEADERS });
  const walletId = walletRes.json('walletId');

  http.post(`${WALLET_URL}/wallets/${walletId}/transactions`,
    JSON.stringify({ walletId, value: 999999.00, type: 'DEPOSIT', exchangeConfirmationId: randomUUID() }),
    { headers: HEADERS });

  // Aguarda projeções do setup
  sleep(3);

  return { accountManagerId, customerId, walletId };
}

export default function (data) {
  console.log(`Iniciando benchmark — ${ITERATIONS} iterações por método`);

  // 1. Criar AccountManager
  for (let i = 0; i < ITERATIONS; i++) {
    const t0 = Date.now();
    http.post(`${STAFF_URL}/account-managers`,
      JSON.stringify({ name: `AM Bench ${i}`, branchId: FIXED_BRANCH_ID, isAdmin: false }),
      { headers: HEADERS });
    tCreateAccountManager.add(Date.now() - t0);
  }
  console.log('✓ create_account_manager concluído');

  // 2. Criar Customer (valida accountManagerId via read app)
  for (let i = 0; i < ITERATIONS; i++) {
    const t0 = Date.now();
    http.post(`${CUSTOMER_URL}/customers`,
      JSON.stringify({ name: `Customer Bench ${i}`, address: `Rua ${i}`, age: 30, accountManagerId: data.accountManagerId }),
      { headers: HEADERS });
    tCreateCustomer.add(Date.now() - t0);
  }
  console.log('✓ create_customer concluído');

  // 3. Criar Wallet (valida customerId via read app)
  for (let i = 0; i < ITERATIONS; i++) {
    const t0 = Date.now();
    http.post(`${WALLET_URL}/wallets`,
      JSON.stringify({ customerId: data.customerId }),
      { headers: HEADERS });
    tCreateWallet.add(Date.now() - t0);
  }
  console.log('✓ create_wallet concluído');

  // 4. Registrar transação
  for (let i = 0; i < ITERATIONS; i++) {
    const t0 = Date.now();
    http.post(`${WALLET_URL}/wallets/${data.walletId}/transactions`,
      JSON.stringify({ walletId: data.walletId, value: 1.00, type: 'DEPOSIT', exchangeConfirmationId: randomUUID() }),
      { headers: HEADERS });
    tRegisterTransaction.add(Date.now() - t0);
  }
  console.log('✓ register_transaction concluído');

  // 5. Ler AccountManager por ID
  for (let i = 0; i < ITERATIONS; i++) {
    const t0 = Date.now();
    http.get(`${READ_URL}/account-managers/${data.accountManagerId}`);
    tReadAccountManager.add(Date.now() - t0);
  }
  console.log('✓ read_account_manager concluído');

  // 6. Ler Customer por ID
  for (let i = 0; i < ITERATIONS; i++) {
    const t0 = Date.now();
    http.get(`${READ_URL}/customers/${data.customerId}`);
    tReadCustomer.add(Date.now() - t0);
  }
  console.log('✓ read_customer concluído');

  // 7. Listar todos os Customers
  for (let i = 0; i < ITERATIONS; i++) {
    const t0 = Date.now();
    http.get(`${READ_URL}/customers`);
    tListCustomers.add(Date.now() - t0);
  }
  console.log('✓ list_customers concluído');
}

export function handleSummary(data) {
  const methods = [
    ['create_account_manager', 'exec_create_account_manager_ms'],
    ['create_customer',        'exec_create_customer_ms'],
    ['create_wallet',          'exec_create_wallet_ms'],
    ['register_transaction',   'exec_register_transaction_ms'],
    ['read_account_manager',   'exec_read_account_manager_ms'],
    ['read_customer',          'exec_read_customer_ms'],
    ['list_customers',         'exec_list_customers_ms'],
  ];

  console.log(`\n${'='.repeat(60)}`);
  console.log(`BENCHMARK — TEMPO MÉDIO DE EXECUÇÃO (${ITERATIONS} iterações)`);
  console.log('='.repeat(60));
  console.log(`${'Método'.padEnd(30)} ${'Média (ms)'.padStart(10)} ${'Min (ms)'.padStart(10)} ${'Max (ms)'.padStart(10)}`);
  console.log('-'.repeat(60));

  for (const [name, key] of methods) {
    const m = data.metrics[key];
    if (m) {
      const avg = m.values.avg.toFixed(2);
      const min = m.values.min.toFixed(2);
      const max = m.values.max.toFixed(2);
      console.log(`${name.padEnd(30)} ${avg.padStart(10)} ${min.padStart(10)} ${max.padStart(10)}`);
    }
  }

  console.log('='.repeat(60));
  return {};
}

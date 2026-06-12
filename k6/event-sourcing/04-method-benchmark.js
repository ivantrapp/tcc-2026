/**
 * CQRS Event Sourcing — Method Benchmark Under Concurrency
 * Cada endpoint é testado isoladamente com múltiplos VUs concorrentes por 30s.
 * Os cenários são executados em sequência para evitar interferência entre si.
 *
 * Uso:
 *   k6 run 04-method-benchmark.js
 *   k6 run -e VUS=50 -e DURATION=30s 04-method-benchmark.js
 *   k6 run -e VUS=100 -e DURATION=60s -e STAFF_URL=http://localhost:8082 -e CUSTOMER_URL=http://localhost:8083 -e WALLET_URL=http://localhost:8084 -e READ_URL=http://localhost:8081 04-method-benchmark.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const STAFF_URL    = __ENV.STAFF_URL    || 'http://localhost:8082';
const CUSTOMER_URL = __ENV.CUSTOMER_URL || 'http://localhost:8083';
const WALLET_URL   = __ENV.WALLET_URL   || 'http://localhost:8084';
const READ_URL     = __ENV.READ_URL     || 'http://localhost:8081';
const VUS          = parseInt(__ENV.VUS      || '50');
const DURATION     = __ENV.DURATION          || '30s';
const HEADERS      = { 'Content-Type': 'application/json' };

const FIXED_BRANCH_ID = '00000000-0000-0000-0000-000000000001';

// Cada método possui sua própria Trend para análise individual
const tCreateAccountManager = new Trend('exec_create_account_manager_ms', true);
const tCreateCustomer       = new Trend('exec_create_customer_ms',        true);
const tCreateWallet         = new Trend('exec_create_wallet_ms',          true);
const tRegisterTransaction  = new Trend('exec_register_transaction_ms',   true);
const tReadAccountManager   = new Trend('exec_read_account_manager_ms',   true);
const tReadCustomer         = new Trend('exec_read_customer_ms',          true);
const tListCustomers        = new Trend('exec_list_customers_ms',         true);

// Cada cenário roda por DURATION com VUS concorrentes; gap de 5s entre cenários
const STEP = 35;

export const options = {
  summaryTrendStats: ['min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    bench_create_account_manager: {
      executor: 'constant-vus', vus: VUS, duration: DURATION,
      startTime: `${STEP * 0}s`, exec: 'benchCreateAccountManager',
    },
    bench_create_customer: {
      executor: 'constant-vus', vus: VUS, duration: DURATION,
      startTime: `${STEP * 1}s`, exec: 'benchCreateCustomer',
    },
    bench_create_wallet: {
      executor: 'constant-vus', vus: VUS, duration: DURATION,
      startTime: `${STEP * 2}s`, exec: 'benchCreateWallet',
    },
    bench_register_transaction: {
      executor: 'constant-vus', vus: VUS, duration: DURATION,
      startTime: `${STEP * 3}s`, exec: 'benchRegisterTransaction',
    },
    bench_read_account_manager: {
      executor: 'constant-vus', vus: VUS, duration: DURATION,
      startTime: `${STEP * 4}s`, exec: 'benchReadAccountManager',
    },
    bench_read_customer: {
      executor: 'constant-vus', vus: VUS, duration: DURATION,
      startTime: `${STEP * 5}s`, exec: 'benchReadCustomer',
    },
    bench_list_customers: {
      executor: 'constant-vus', vus: VUS, duration: DURATION,
      startTime: `${STEP * 6}s`, exec: 'benchListCustomers',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
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

  // Pool de customers para bench_create_wallet (um por VU evita contenção)
  const customerPool = [];
  for (let i = 0; i < VUS; i++) {
    const custRes = http.post(`${CUSTOMER_URL}/customers`,
      JSON.stringify({ name: `Customer Pool ${i}`, address: `Rua Pool ${i}`, age: 30, accountManagerId }),
      { headers: HEADERS });
    const customerId = custRes.json('customerId');
    if (customerId) customerPool.push(customerId);
  }

  // Pool de wallets para bench_register_transaction (um por VU)
  const walletPool = [];
  for (let i = 0; i < customerPool.length; i++) {
    const walletRes = http.post(`${WALLET_URL}/wallets`,
      JSON.stringify({ customerId: customerPool[i] }), { headers: HEADERS });
    const walletId = walletRes.json('walletId');
    if (!walletId) continue;

    http.post(`${WALLET_URL}/wallets/${walletId}/transactions`,
      JSON.stringify({ walletId, value: 999999.00, type: 'DEPOSIT', exchangeConfirmationId: randomUUID() }),
      { headers: HEADERS });
    walletPool.push(walletId);
  }

  // Aguarda projeções do setup chegarem na read app
  sleep(5);

  console.log(`[setup] pool: ${customerPool.length} customers, ${walletPool.length} wallets`);
  return { accountManagerId, customerPool, walletPool };
}

export function benchCreateAccountManager(data) {
  const t0 = Date.now();
  const res = http.post(`${STAFF_URL}/account-managers`,
    JSON.stringify({ name: `AM ${__VU}_${__ITER}`, branchId: FIXED_BRANCH_ID, isAdmin: false }),
    { headers: HEADERS });
  tCreateAccountManager.add(Date.now() - t0);
  check(res, { 'create_account_manager ok': (r) => r.status === 200 || r.status === 201 });
}

export function benchCreateCustomer(data) {
  const t0 = Date.now();
  const res = http.post(`${CUSTOMER_URL}/customers`,
    JSON.stringify({ name: `Customer ${__VU}_${__ITER}`, address: `Rua ${__VU}`, age: 30, accountManagerId: data.accountManagerId }),
    { headers: HEADERS });
  tCreateCustomer.add(Date.now() - t0);
  check(res, { 'create_customer ok': (r) => r.status === 200 || r.status === 201 });
}

export function benchCreateWallet(data) {
  // Rotaciona pelo pool para que cada VU use um customer diferente
  const customerId = data.customerPool[(__VU - 1) % data.customerPool.length];
  const t0 = Date.now();
  const res = http.post(`${WALLET_URL}/wallets`,
    JSON.stringify({ customerId }), { headers: HEADERS });
  tCreateWallet.add(Date.now() - t0);
  check(res, { 'create_wallet ok': (r) => r.status === 200 || r.status === 201 });
}

export function benchRegisterTransaction(data) {
  const walletId = data.walletPool[(__VU - 1) % data.walletPool.length];
  const t0 = Date.now();
  const res = http.post(`${WALLET_URL}/wallets/${walletId}/transactions`,
    JSON.stringify({ walletId, value: 1.00, type: 'DEPOSIT', exchangeConfirmationId: randomUUID() }),
    { headers: HEADERS });
  tRegisterTransaction.add(Date.now() - t0);
  check(res, { 'register_transaction ok': (r) => r.status === 201 });
}

export function benchReadAccountManager(data) {
  const t0 = Date.now();
  const res = http.get(`${READ_URL}/account-managers/${data.accountManagerId}`);
  tReadAccountManager.add(Date.now() - t0);
  check(res, { 'read_account_manager ok': (r) => r.status === 200 });
}

export function benchReadCustomer(data) {
  const customerId = data.customerPool[(__VU - 1) % data.customerPool.length];
  const t0 = Date.now();
  const res = http.get(`${READ_URL}/customers/${customerId}`);
  tReadCustomer.add(Date.now() - t0);
  check(res, { 'read_customer ok': (r) => r.status === 200 });
}

export function benchListCustomers(data) {
  const t0 = Date.now();
  const res = http.get(`${READ_URL}/customers`);
  tListCustomers.add(Date.now() - t0);
  check(res, { 'list_customers ok': (r) => r.status === 200 });
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

  console.log(`\n${'='.repeat(70)}`);
  console.log(`BENCHMARK — ${VUS} VUs CONCORRENTES | DURAÇÃO: ${DURATION} por método`);
  console.log('='.repeat(70));
  console.log(`${'Método'.padEnd(30)} ${'p(50)'.padStart(8)} ${'p(95)'.padStart(8)} ${'p(99)'.padStart(8)} ${'max'.padStart(8)}`);
  console.log('-'.repeat(70));

  for (const [name, key] of methods) {
    const m = data.metrics[key];
    if (m) {
      const p50 = m.values['med'].toFixed(1);
      const p95 = m.values['p(95)'].toFixed(1);
      const p99 = m.values['p(99)'].toFixed(1);
      const max = m.values.max.toFixed(1);
      console.log(`${name.padEnd(30)} ${p50.padStart(8)} ${p95.padStart(8)} ${p99.padStart(8)} ${max.padStart(8)}`);
    }
  }

  console.log('='.repeat(70));
  return {};
}

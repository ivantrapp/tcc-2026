/**
 * CQRS Event Sourcing — Projection Lag Test
 * Mede o tempo entre uma escrita no write DB (via evento RabbitMQ)
 * e a visibilidade no read DB (projeção aplicada).
 *
 * Métrica customizada: projection_lag_ms (p50, p95, p99, max)
 *
 * Uso:
 *   k6 run 03-lag.js
 *   k6 run -e ITERATIONS=50 03-lag.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const STAFF_URL    = __ENV.STAFF_URL    || 'http://localhost:8082';
const CUSTOMER_URL = __ENV.CUSTOMER_URL || 'http://localhost:8083';
const WALLET_URL   = __ENV.WALLET_URL   || 'http://localhost:8084';
const READ_URL     = __ENV.READ_URL     || 'http://localhost:8081';
const ITERATIONS   = parseInt(__ENV.ITERATIONS || '100');
const HEADERS      = { 'Content-Type': 'application/json' };

const FIXED_BRANCH_ID = '00000000-0000-0000-0000-000000000001';

const projectionLag = new Trend('projection_lag_ms', true);
const lagTimeouts   = new Counter('projection_timeouts');

export const options = {
  scenarios: {
    lag_test: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: ITERATIONS,
    },
  },
  thresholds: {
    projection_lag_ms:   ['p(95)<2000', 'p(99)<5000'],
    projection_timeouts: ['count<3'],
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
    JSON.stringify({ name: 'Gerente Lag ES K6', branchId: FIXED_BRANCH_ID, isAdmin: false }),
    { headers: HEADERS });
  const accountManagerId = amRes.json('accountManagerId');

  const custRes = http.post(`${CUSTOMER_URL}/customers`,
    JSON.stringify({ name: 'Cliente Lag ES', address: 'Rua Lag ES, 1', age: 30, accountManagerId }),
    { headers: HEADERS });
  const customerId = custRes.json('customerId');

  const walletRes = http.post(`${WALLET_URL}/wallets`,
    JSON.stringify({ customerId }), { headers: HEADERS });
  const walletId = walletRes.json('walletId');

  // Depósito inicial
  http.post(`${WALLET_URL}/wallets/${walletId}/transactions`,
    JSON.stringify({ walletId, value: 100000.00, type: 'DEPOSIT', exchangeConfirmationId: randomUUID() }),
    { headers: HEADERS });

  // Aguarda a projeção inicial ser processada pelo RabbitMQ consumer
  sleep(5);

  return { customerId, walletId };
}

export default function (data) {
  // 1. Lê saldo atual no read side (wallet_balance na customer_view)
  const beforeRes = http.get(`${READ_URL}/customers/${data.customerId}`);
  check(beforeRes, { 'read before ok': (r) => r.status === 200 });
  const balanceBefore = beforeRes.json('walletBalance');

  // 2. Registra transação no wallet service
  const depositValue = 1.00 + __ITER;
  const startMs = Date.now();

  const txRes = http.post(
    `${WALLET_URL}/wallets/${data.walletId}/transactions`,
    JSON.stringify({
      walletId: data.walletId,
      value: depositValue,
      type: 'DEPOSIT',
      exchangeConfirmationId: randomUUID(),
    }),
    { headers: HEADERS }
  );
  check(txRes, { 'write 201': (r) => r.status === 201 });

  // 3. Polling no read side até wallet_balance mudar (max 10s)
  let lagMs = -1;
  for (let attempt = 0; attempt < 200; attempt++) {
    sleep(0.05);
    const afterRes = http.get(`${READ_URL}/customers/${data.customerId}`);
    if (afterRes.status === 200) {
      const balanceAfter = afterRes.json('walletBalance');
      if (balanceAfter > balanceBefore) {
        lagMs = Date.now() - startMs;
        break;
      }
    }
  }

  if (lagMs === -1) {
    lagTimeouts.add(1);
    console.warn(`[iter ${__ITER}] Timeout: projeção não aplicada em 10s`);
  } else {
    projectionLag.add(lagMs);
    console.log(`[iter ${__ITER}] Lag de projeção: ${lagMs}ms`);
  }

  sleep(2);
}

export function handleSummary(data) {
  const lag = data.metrics['projection_lag_ms'];
  if (!lag) {
    console.log('Nenhuma medição de lag registrada.');
    return {};
  }
  console.log('\n=== PROJECTION LAG (ms) ===');
  console.log(`  min  : ${lag.values.min.toFixed(0)}`);
  console.log(`  med  : ${lag.values['med'].toFixed(0)}`);
  console.log(`  p(90): ${lag.values['p(90)'].toFixed(0)}`);
  console.log(`  p(95): ${lag.values['p(95)'].toFixed(0)}`);
  console.log(`  p(99): ${lag.values['p(99)'].toFixed(0)}`);
  console.log(`  max  : ${lag.values.max.toFixed(0)}`);
  return {};
}

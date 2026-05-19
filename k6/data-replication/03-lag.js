/**
 * CQRS Data Replication — Replication Lag Test
 * Mede o tempo entre uma escrita no write DB e a visibilidade no read DB via WAL.
 *
 * Métrica customizada: replication_lag_ms (p50, p95, p99, max)
 *
 * Uso:
 *   k6 run 03-lag.js
 *   k6 run -e ITERATIONS=50 03-lag.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const WRITE_URL  = __ENV.WRITE_URL  || 'http://localhost:8080';
const READ_URL   = __ENV.READ_URL   || 'http://localhost:8081';
const ITERATIONS = parseInt(__ENV.ITERATIONS || '100');
const HEADERS    = { 'Content-Type': 'application/json' };

const replicationLag = new Trend('replication_lag_ms', true);
const lagTimeouts    = new Counter('replication_timeouts');

export const options = {
  scenarios: {
    lag_test: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: ITERATIONS,
    },
  },
  thresholds: {
    replication_lag_ms:  ['p(95)<2000'],  // 95% das replicações < 2s
    replication_timeouts: ['count<3'],    // máximo 3 timeouts em todo o teste
  },
};

function randomUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = Math.random() * 16 | 0;
    return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
  });
}

export function setup() {
  const branchRes = http.post(`${WRITE_URL}/branches`,
    JSON.stringify({ region: 'Centro-Oeste' }), { headers: HEADERS });
  const branchId = branchRes.json('branchId');

  const amRes = http.post(`${WRITE_URL}/account-managers`,
    JSON.stringify({ name: 'Gerente Lag K6', branchId, isAdmin: false }), { headers: HEADERS });
  const accountManagerId = amRes.json('accountManagerId');

  const custRes = http.post(`${WRITE_URL}/customers`,
    JSON.stringify({ name: 'Cliente Lag', address: 'Rua Lag, 1', age: 30, accountManagerId }),
    { headers: HEADERS });
  const customerId = custRes.json('customerId');

  const walletRes = http.post(`${WRITE_URL}/wallets`,
    JSON.stringify({ customerId }), { headers: HEADERS });
  const walletId = walletRes.json('walletId');

  // Depósito inicial
  http.post(`${WRITE_URL}/wallets/${walletId}/transactions`,
    JSON.stringify({ walletId, value: 100000.00, type: 'DEPOSIT', exchangeConfirmationId: randomUUID() }),
    { headers: HEADERS });

  // Aguarda replicação do saldo inicial antes de iniciar o teste
  sleep(5);

  return { customerId, walletId };
}

export default function (data) {
  // 1. Lê saldo atual no read side
  const beforeRes = http.get(`${READ_URL}/customers/${data.customerId}`);
  check(beforeRes, { 'read before ok': (r) => r.status === 200 });
  const balanceBefore = beforeRes.json('totalBalance');

  // 2. Registra transação no write side e marca o tempo
  const depositValue = 1.00 + __ITER; // valor único por iteração
  const startMs = Date.now();

  const txRes = http.post(
    `${WRITE_URL}/wallets/${data.walletId}/transactions`,
    JSON.stringify({
      walletId: data.walletId,
      value: depositValue,
      type: 'DEPOSIT',
      exchangeConfirmationId: randomUUID(),
    }),
    { headers: HEADERS }
  );
  check(txRes, { 'write 201': (r) => r.status === 201 });

  // 3. Polling no read side até o saldo mudar (max 10s)
  let lagMs = -1;
  for (let attempt = 0; attempt < 200; attempt++) {
    sleep(0.05); // 50ms entre tentativas
    const afterRes = http.get(`${READ_URL}/customers/${data.customerId}`);
    if (afterRes.status === 200) {
      const balanceAfter = afterRes.json('totalBalance');
      if (balanceAfter > balanceBefore) {
        lagMs = Date.now() - startMs;
        break;
      }
    }
  }

  if (lagMs === -1) {
    lagTimeouts.add(1);
    console.warn(`[iter ${__ITER}] Timeout: saldo não replicado em 10s`);
  } else {
    replicationLag.add(lagMs);
    console.log(`[iter ${__ITER}] Lag de replicação: ${lagMs}ms`);
  }

  // Pausa entre iterações para não acumular saldo artificialmente rápido
  sleep(2);
}

export function handleSummary(data) {
  const lag = data.metrics['replication_lag_ms'];
  if (!lag) {
    console.log('Nenhuma medição de lag registrada.');
    return {};
  }
  console.log('\n=== REPLICATION LAG (ms) ===');
  console.log(`  min  : ${lag.values.min.toFixed(0)}`);
  console.log(`  med  : ${lag.values['med'].toFixed(0)}`);
  console.log(`  p(90): ${lag.values['p(90)'].toFixed(0)}`);
  console.log(`  p(95): ${lag.values['p(95)'].toFixed(0)}`);
  console.log(`  p(99): ${lag.values['p(99)'].toFixed(0)}`);
  console.log(`  max  : ${lag.values.max.toFixed(0)}`);
  return {};
}

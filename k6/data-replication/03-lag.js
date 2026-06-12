/**
 * CQRS Data Replication — Replication Lag Under Load
 * Mede o lag de replicação WAL com múltiplos VUs escrevendo e lendo concorrentemente.
 * Cada VU possui sua própria carteira para isolar as medições.
 *
 * Uso:
 *   k6 run 03-lag.js
 *   k6 run -e VUS=50 03-lag.js
 *   k6 run -e VUS=20 -e STAFF_URL=http://localhost:8080 -e CUSTOMER_URL=http://localhost:8080 -e WALLET_URL=http://localhost:8080 03-lag.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const STAFF_URL    = __ENV.STAFF_URL    || 'http://localhost:8082';
const CUSTOMER_URL = __ENV.CUSTOMER_URL || 'http://localhost:8083';
const WALLET_URL   = __ENV.WALLET_URL   || 'http://localhost:8084';
const READ_URL     = __ENV.READ_URL     || 'http://localhost:8081';
const VUS          = parseInt(__ENV.VUS || '20');
const HEADERS      = { 'Content-Type': 'application/json' };

const replicationLag = new Trend('replication_lag_ms', true);
const lagTimeouts    = new Counter('replication_timeouts');

export const options = {
  summaryTrendStats: ['min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    lag_under_load: {
      executor: 'constant-vus',
      vus: VUS,
      duration: '3m',
    },
  },
  thresholds: {
    replication_lag_ms:   ['p(95)<2000', 'p(99)<5000'],
    replication_timeouts: ['count<10'],
    http_req_failed:      ['rate<0.05'],
  },
};

function randomUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = Math.random() * 16 | 0;
    return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
  });
}

export function setup() {
  const branchRes = http.post(`${STAFF_URL}/branches`,
    JSON.stringify({ region: 'Centro-Oeste' }), { headers: HEADERS });
  const branchId = branchRes.json('branchId');

  const amRes = http.post(`${STAFF_URL}/account-managers`,
    JSON.stringify({ name: 'Gerente Lag K6', branchId, isAdmin: false }), { headers: HEADERS });
  const accountManagerId = amRes.json('accountManagerId');

  const entities = [];
  for (let i = 0; i < VUS; i++) {
    const custRes = http.post(`${CUSTOMER_URL}/customers`,
      JSON.stringify({ name: `Cliente Lag ${i}`, address: `Rua Lag ${i}, 1`, age: 30, accountManagerId }),
      { headers: HEADERS });
    const customerId = custRes.json('customerId');
    if (!customerId) { console.warn(`[setup] customer ${i} falhou`); continue; }

    const walletRes = http.post(`${WALLET_URL}/wallets`,
      JSON.stringify({ customerId }), { headers: HEADERS });
    const walletId = walletRes.json('walletId');
    if (!walletId) { console.warn(`[setup] wallet ${i} falhou`); continue; }

    http.post(`${WALLET_URL}/wallets/${walletId}/transactions`,
      JSON.stringify({ walletId, value: 100000.00, type: 'DEPOSIT', exchangeConfirmationId: randomUUID() }),
      { headers: HEADERS });

    entities.push({ customerId, walletId });
  }

  sleep(5);
  console.log(`[setup] ${entities.length}/${VUS} entidades prontas`);
  return { entities };
}

export default function (data) {
  if (!data.entities.length) return;

  const entity = data.entities[(__VU - 1) % data.entities.length];

  const beforeRes = http.get(`${READ_URL}/customers/${entity.customerId}`);
  check(beforeRes, { 'read before ok': (r) => r.status === 200 });
  const balanceBefore = beforeRes.json('totalBalance');

  const startMs = Date.now();
  const txRes = http.post(
    `${WALLET_URL}/wallets/${entity.walletId}/transactions`,
    JSON.stringify({ walletId: entity.walletId, value: 1.00, type: 'DEPOSIT', exchangeConfirmationId: randomUUID() }),
    { headers: HEADERS }
  );
  check(txRes, { 'write 201': (r) => r.status === 201 });

  let lagMs = -1;
  for (let attempt = 0; attempt < 200; attempt++) {
    sleep(0.05);
    const afterRes = http.get(`${READ_URL}/customers/${entity.customerId}`);
    if (afterRes.status === 200 && afterRes.json('totalBalance') > balanceBefore) {
      lagMs = Date.now() - startMs;
      break;
    }
  }

  if (lagMs === -1) {
    lagTimeouts.add(1);
    console.warn(`[VU ${__VU}] Timeout: saldo não replicado em 10s`);
  } else {
    replicationLag.add(lagMs);
  }

  sleep(1);
}

export function handleSummary(data) {
  const lag = data.metrics['replication_lag_ms'];
  if (!lag) { console.log('Nenhuma medição de lag registrada.'); return {}; }
  console.log('\n=== REPLICATION LAG UNDER LOAD (ms) ===');
  console.log(`  VUs  : ${VUS}`);
  console.log(`  min  : ${lag.values.min.toFixed(0)}`);
  console.log(`  med  : ${lag.values['med'].toFixed(0)}`);
  console.log(`  p(90): ${lag.values['p(90)'].toFixed(0)}`);
  console.log(`  p(95): ${lag.values['p(95)'].toFixed(0)}`);
  console.log(`  p(99): ${lag.values['p(99)'].toFixed(0)}`);
  console.log(`  max  : ${lag.values.max.toFixed(0)}`);
  return {};
}

/**
 * CQRS Event Sourcing — Write Load Test
 * Mede throughput e latência dos microsserviços de escrita sob carga de transações.
 *
 * Uso:
 *   k6 run 01-write-load.js
 *   k6 run -e STAFF_URL=http://localhost:8082 -e CUSTOMER_URL=http://localhost:8083 -e WALLET_URL=http://localhost:8084 01-write-load.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

const STAFF_URL    = __ENV.STAFF_URL    || 'http://localhost:8082';
const CUSTOMER_URL = __ENV.CUSTOMER_URL || 'http://localhost:8083';
const WALLET_URL   = __ENV.WALLET_URL   || 'http://localhost:8084';
const HEADERS      = { 'Content-Type': 'application/json' };

const FIXED_BRANCH_ID = '00000000-0000-0000-0000-000000000001';

export const options = {
  scenarios: {
    write_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 100 },
        { duration: '60s', target: 100 },
        { duration: '30s', target: 200 },
        { duration: '60s', target: 200 },
        { duration: '30s', target: 300 },
        { duration: '60s', target: 300 },
        { duration: '30s', target:   0 },
      ],
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
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
    JSON.stringify({ name: 'Gerente K6 ES', branchId: FIXED_BRANCH_ID, isAdmin: true }),
    { headers: HEADERS });
  const accountManagerId = amRes.json('accountManagerId');

  const wallets = [];
  for (let i = 0; i < 50; i++) {
    const custRes = http.post(`${CUSTOMER_URL}/customers`,
      JSON.stringify({ name: `Cliente Write ES ${i}`, address: `Rua ${i}, 100`, age: 30 + i, accountManagerId }),
      { headers: HEADERS });
    const customerId = custRes.json('customerId');
    if (!customerId) {
      console.warn(`[setup] customer ${i} falhou (status ${custRes.status})`);
      continue;
    }

    const walletRes = http.post(`${WALLET_URL}/wallets`,
      JSON.stringify({ customerId }), { headers: HEADERS });
    const walletId = walletRes.json('walletId');
    if (!walletId) {
      console.warn(`[setup] wallet ${i} falhou (status ${walletRes.status})`);
      continue;
    }

    http.post(`${WALLET_URL}/wallets/${walletId}/transactions`,
      JSON.stringify({ walletId, value: 50000.00, type: 'DEPOSIT', exchangeConfirmationId: randomUUID() }),
      { headers: HEADERS });

    wallets.push(walletId);
  }

  console.log(`[setup] ${wallets.length}/50 wallets criados com sucesso`);
  return { wallets };
}

export default function (data) {
  if (!data.wallets.length) return;

  const walletId = data.wallets[__VU % data.wallets.length];
  const type     = __ITER % 3 === 0 ? 'WITHDRAWAL' : 'DEPOSIT';

  const res = http.post(
    `${WALLET_URL}/wallets/${walletId}/transactions`,
    JSON.stringify({ walletId, value: 100.00, type, exchangeConfirmationId: randomUUID() }),
    { headers: HEADERS }
  );

  // 409 = ConcurrencyException (optimistic locking) — comportamento esperado sob alta concorrência
  check(res, { 'status 201 ou 409': (r) => r.status === 201 || r.status === 409 });
}

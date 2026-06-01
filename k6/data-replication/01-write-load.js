/**
 * CQRS Data Replication — Write Load Test (microsserviços)
 * Mede throughput e latência do write side sob carga de transações.
 *
 * Uso (microsserviços — padrão):
 *   k6 run 01-write-load.js
 *
 * Uso (monolito):
 *   k6 run \
 *     -e STAFF_URL=http://localhost:8080 \
 *     -e CUSTOMER_URL=http://localhost:8080 \
 *     -e WALLET_URL=http://localhost:8080 \
 *     01-write-load.js
 */

import http from 'k6/http';
import { check } from 'k6';

const STAFF_URL    = __ENV.STAFF_URL    || 'http://localhost:8082';
const CUSTOMER_URL = __ENV.CUSTOMER_URL || 'http://localhost:8083';
const WALLET_URL   = __ENV.WALLET_URL   || 'http://localhost:8084';
const HEADERS      = { 'Content-Type': 'application/json' };

export const options = {
  scenarios: {
    write_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 100 },
        { duration: '60s', target: 200 },
        { duration: '30s', target: 200 },
        { duration: '15s', target:   0 },
      ],
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.05'],
    http_req_duration: ['p(95)<1000'],
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
    JSON.stringify({ region: 'Sudeste' }), { headers: HEADERS });
  const branchId = branchRes.json('branchId');

  const amRes = http.post(`${STAFF_URL}/account-managers`,
    JSON.stringify({ name: 'Gerente K6', branchId, isAdmin: true }), { headers: HEADERS });
  const accountManagerId = amRes.json('accountManagerId');

  const wallets = [];
  for (let i = 0; i < 50; i++) {
    const custRes = http.post(`${CUSTOMER_URL}/customers`,
      JSON.stringify({ name: `Cliente Write ${i}`, address: `Rua ${i}, 100`, age: 30 + i, accountManagerId }),
      { headers: HEADERS });
    const customerId = custRes.json('customerId');

    const walletRes = http.post(`${WALLET_URL}/wallets`,
      JSON.stringify({ customerId }), { headers: HEADERS });
    const walletId = walletRes.json('walletId');

    // Saldo inicial para permitir WITHDRAWALs
    http.post(`${WALLET_URL}/wallets/${walletId}/transactions`,
      JSON.stringify({ walletId, value: 50000.00, type: 'DEPOSIT', exchangeConfirmationId: randomUUID() }),
      { headers: HEADERS });

    wallets.push(walletId);
  }

  return { wallets };
}

export default function (data) {
  const walletId = data.wallets[__VU % data.wallets.length];
  const type     = __ITER % 3 === 0 ? 'WITHDRAWAL' : 'DEPOSIT';

  const res = http.post(
    `${WALLET_URL}/wallets/${walletId}/transactions`,
    JSON.stringify({ walletId, value: 100.00, type, exchangeConfirmationId: randomUUID() }),
    { headers: HEADERS }
  );

  check(res, { 'status 201': (r) => r.status === 201 });
}

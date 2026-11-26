/**
 * CQRS Event Sourcing — Read Load Test
 *
 * Uso:
 *   k6 run 02-read-load.js
 *   k6 run -e STAFF_URL=http://localhost:8082 -e CUSTOMER_URL=http://localhost:8083 -e WALLET_URL=http://localhost:8084 -e READ_URL=http://localhost:8081 02-read-load.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

const STAFF_URL    = __ENV.STAFF_URL    || 'http://localhost:8082';
const CUSTOMER_URL = __ENV.CUSTOMER_URL || 'http://localhost:8083';
const WALLET_URL   = __ENV.WALLET_URL   || 'http://localhost:8084';
const READ_URL     = __ENV.READ_URL     || 'http://localhost:8081';
const HEADERS      = { 'Content-Type': 'application/json' };

export const options = {
  summaryTrendStats: ['min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    read_load: {
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
    http_req_duration: ['p(95)<500'],
  },
};

function randomUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = Math.random() * 16 | 0;
    return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
  });
}

// Espera o lado de leitura refletir uma escrita (consistencia eventual). As
// validacoes de escrita consultam o read (gerente existe? cliente existe?), entao
// a preparacao so avanca quando o dado ja esta visivel la.
function waitForRead(url, isReady, timeoutSec = 120) {
  const deadline = Date.now() + timeoutSec * 1000;
  while (Date.now() < deadline) {
    const res = http.get(url, { tags: { name: 'setup-wait' }, responseCallback: http.expectedStatuses(200, 404) });
    if (res.status === 200 && isReady(res)) return true;
    sleep(0.5);
  }
  console.warn(`[setup] timeout esperando ${url}`);
  return false;
}

export function setup() {
  const branchRes = http.post(`${STAFF_URL}/branches`,
    JSON.stringify({ region: 'Sul' }), { headers: HEADERS });
  const branchId = branchRes.json('branchId');

  const amRes = http.post(`${STAFF_URL}/account-managers`,
    JSON.stringify({ name: 'Gerente Read K6', branchId, isAdmin: true }),
    { headers: HEADERS });
  const accountManagerId = amRes.json('accountManagerId');
  waitForRead(`${READ_URL}/account-managers/${accountManagerId}`, () => true);

  const customerIds = [];
  for (let i = 0; i < 50; i++) {
    const custRes = http.post(`${CUSTOMER_URL}/customers`,
      JSON.stringify({ name: `Cliente Read ${i}`, address: `Av ${i}, 200`, age: 25 + (i % 50), accountManagerId }),
      { headers: HEADERS });
    const customerId = custRes.json('customerId');
    if (!customerId) { console.warn(`[setup] customer ${i} falhou (status ${custRes.status})`); continue; }
    waitForRead(`${READ_URL}/customers/${customerId}`, () => true);

    const walletRes = http.post(`${WALLET_URL}/wallets`,
      JSON.stringify({ customerId }), { headers: HEADERS });
    const walletId = walletRes.json('walletId');
    if (!walletId) { customerIds.push(customerId); continue; }

    for (let t = 0; t < 5; t++) {
      http.post(`${WALLET_URL}/wallets/${walletId}/transactions`,
        JSON.stringify({ walletId, value: (t + 1) * 1000.00, type: 'DEPOSIT', exchangeConfirmationId: randomUUID() }),
        { headers: HEADERS });
    }

    customerIds.push(customerId);
  }

  sleep(5);
  console.log(`[setup] ${customerIds.length}/50 customers prontos`);
  return { customerIds, accountManagerId };
}

export default function (data) {
  if (!data.customerIds.length) return;

  const roll = Math.random();
  const id   = data.customerIds[Math.floor(Math.random() * data.customerIds.length)];

  if (roll < 0.50) {
    const res = http.get(`${READ_URL}/customers/by-account-manager/${data.accountManagerId}`);
    check(res, { 'list by-account-manager 200': (r) => r.status === 200 });

  } else if (roll < 0.75) {
    const res = http.get(`${READ_URL}/customers/${id}`);
    check(res, { 'detail 200': (r) => r.status === 200 });

  } else {
    const res = http.get(`${READ_URL}/customers`);
    check(res, { 'all 200': (r) => r.status === 200 });
  }
}

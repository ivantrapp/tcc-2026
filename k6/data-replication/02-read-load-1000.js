/**
 * CQRS Data Replication — Read Load Test (1000 VUs)
 *
 * k6 run 02-read-load-1000.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

const STAFF_URL    = __ENV.STAFF_URL    || 'http://localhost:8082';
const CUSTOMER_URL = __ENV.CUSTOMER_URL || 'http://localhost:8083';
const WALLET_URL   = __ENV.WALLET_URL   || 'http://localhost:8084';
const READ_URL     = __ENV.READ_URL     || 'http://localhost:8081';
const HEADERS      = { 'Content-Type': 'application/json' };
const BATCH_SIZE   = 50;

export const options = {
  summaryTrendStats: ['min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    read_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 333 },
        { duration: '60s', target: 333 },
        { duration: '30s', target: 667 },
        { duration: '60s', target: 667 },
        { duration: '30s', target: 1000 },
        { duration: '60s', target: 1000 },
        { duration: '30s', target: 0 },
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

function batchRequests(requests) {
  const results = [];
  for (let i = 0; i < requests.length; i += BATCH_SIZE) {
    const responses = http.batch(requests.slice(i, i + BATCH_SIZE));
    for (let j = 0; j < responses.length; j++) results.push(responses[j]);
  }
  return results;
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
    JSON.stringify({ name: 'Gerente Read K6', branchId, isAdmin: true }), { headers: HEADERS });
  const accountManagerId = amRes.json('accountManagerId');
  waitForRead(`${READ_URL}/account-managers/${accountManagerId}`, () => true);

  const customerReqs = [];
  for (let i = 0; i < 50; i++) {
    customerReqs.push({ method: 'POST', url: `${CUSTOMER_URL}/customers`,
      body: JSON.stringify({ name: `Cliente Read ${i}`, address: `Av ${i}, 200`, age: 25 + (i % 50), accountManagerId }),
      params: { headers: HEADERS } });
  }
  const customerResps = batchRequests(customerReqs);
  const createdCustomers = customerResps.filter((r) => r.status === 201).length;
  waitForRead(`${READ_URL}/customers/by-account-manager/${accountManagerId}`,
    (r) => r.json().length >= createdCustomers);

  const walletReqs = [];
  const customerIds = [];
  for (let i = 0; i < customerResps.length; i++) {
    const customerId = customerResps[i].json('customerId');
    if (!customerId) { console.warn(`[setup] customer ${i} falhou`); continue; }
    customerIds.push(customerId);
    walletReqs.push({ method: 'POST', url: `${WALLET_URL}/wallets`,
      body: JSON.stringify({ customerId }), params: { headers: HEADERS } });
  }
  const walletResps = batchRequests(walletReqs);

  // Os 5 depositos vao em 5 rodadas, um por carteira em cada lote. Enviados juntos
  // para a mesma carteira, competem entre si (no event sourcing, pela mesma versao
  // do agregado, e os perdedores sao rejeitados).
  const walletIds = [];
  for (let i = 0; i < walletResps.length; i++) {
    const walletId = walletResps[i].json('walletId');
    if (!walletId) { console.warn(`[setup] wallet ${i} falhou`); continue; }
    walletIds.push(walletId);
  }
  let depositFailures = 0;
  for (let t = 0; t < 5; t++) {
    const depositReqs = walletIds.map((walletId) => ({
      method: 'POST', url: `${WALLET_URL}/wallets/${walletId}/transactions`,
      body: JSON.stringify({ walletId, value: (t + 1) * 1000.00, type: 'DEPOSIT', exchangeConfirmationId: randomUUID() }),
      params: { headers: HEADERS } }));
    for (const r of batchRequests(depositReqs)) {
      if (r.status !== 201) depositFailures++;
    }
  }
  if (depositFailures) console.warn(`[setup] ${depositFailures} depositos falharam`);

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

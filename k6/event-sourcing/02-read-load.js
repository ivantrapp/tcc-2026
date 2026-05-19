/**
 * CQRS Event Sourcing — Read Load Test
 * Mede throughput e latência do read app sob carga de consultas.
 *
 * Uso:
 *   k6 run 02-read-load.js
 *   k6 run -e WRITE_URL=http://localhost:8080 -e READ_URL=http://localhost:8081 02-read-load.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

const WRITE_URL = __ENV.WRITE_URL || 'http://localhost:8080';
const READ_URL  = __ENV.READ_URL  || 'http://localhost:8081';
const HEADERS   = { 'Content-Type': 'application/json' };

const FIXED_BRANCH_ID = '00000000-0000-0000-0000-000000000001';

export const options = {
  scenarios: {
    read_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 200 },
        { duration: '60s', target: 500 },
        { duration: '30s', target: 500 },
        { duration: '15s', target:   0 },
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

export function setup() {
  const amRes = http.post(`${WRITE_URL}/account-managers`,
    JSON.stringify({ name: 'Gerente Read ES K6', branchId: FIXED_BRANCH_ID, isAdmin: true }),
    { headers: HEADERS });
  const accountManagerId = amRes.json('accountManagerId');

  const customerIds = [];
  for (let i = 0; i < 50; i++) {
    const custRes = http.post(`${WRITE_URL}/customers`,
      JSON.stringify({ name: `Cliente Read ES ${i}`, address: `Av ${i}, 200`, age: 25 + i, accountManagerId }),
      { headers: HEADERS });
    const customerId = custRes.json('customerId');

    const walletRes = http.post(`${WRITE_URL}/wallets`,
      JSON.stringify({ customerId }), { headers: HEADERS });
    const walletId = walletRes.json('walletId');

    for (let t = 0; t < 5; t++) {
      http.post(`${WRITE_URL}/wallets/${walletId}/transactions`,
        JSON.stringify({ walletId, value: (t + 1) * 1000.00, type: 'DEPOSIT', exchangeConfirmationId: randomUUID() }),
        { headers: HEADERS });
    }

    customerIds.push(customerId);
  }

  // Aguarda projeção via RabbitMQ processar os eventos do setup
  sleep(5);

  return { customerIds, accountManagerId };
}

export default function (data) {
  const roll = Math.random();

  if (roll < 0.50) {
    // Listagem por account manager
    const res = http.get(`${READ_URL}/customers/by-account-manager/${data.accountManagerId}`);
    check(res, { 'list 200': (r) => r.status === 200 });

  } else if (roll < 0.80) {
    // Busca individual
    const id = data.customerIds[Math.floor(Math.random() * data.customerIds.length)];
    const res = http.get(`${READ_URL}/customers/${id}`);
    check(res, { 'detail 200': (r) => r.status === 200 });

  } else {
    // Listagem completa
    const res = http.get(`${READ_URL}/customers`);
    check(res, { 'all 200': (r) => r.status === 200 });
  }
}

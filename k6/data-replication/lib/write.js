/**
 * CQRS Data Replication — lógica compartilhada do teste de escrita.
 *
 * O teste roda em duas execuções separadas do k6:
 *   1. 00-write-seed.js  cria N clientes + N carteiras (com depósito inicial)
 *                        e grava os IDs em data/write-wallets-<N>.json;
 *   2. 01-write-load-*.js lê esse arquivo com SharedArray e aplica a carga.
 *
 * Motivos:
 *   - o k6 copia o retorno de setup() para CADA VU. Com 10000 VUs e 10000 IDs
 *     isso passava de 12 GB e derrubava a VM do Docker; o SharedArray guarda
 *     uma única cópia, somente leitura, compartilhada entre os VUs;
 *   - as ~3N requisições de preparação deixam de entrar nas métricas da carga.
 *
 * Cada requisição da carga recebe a tag `stage` (rampa-X, patamar-X, descida),
 * e o resumo mostra latência, erros e contagem de requisições por patamar.
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

const STAFF_URL    = __ENV.STAFF_URL    || 'http://localhost:8082';
const CUSTOMER_URL = __ENV.CUSTOMER_URL || 'http://localhost:8083';
const WALLET_URL   = __ENV.WALLET_URL   || 'http://localhost:8084';
const READ_URL     = __ENV.READ_URL     || 'http://localhost:8081';
const HEADERS      = { 'Content-Type': 'application/json' };
const BATCH_SIZE   = 50;
const DATA_DIR     = __ENV.DATA_DIR     || './data';

export function walletsFile(n) {
  return `${DATA_DIR}/write-wallets-${n}.json`;
}

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
    for (let j = 0; j < responses.length; j++) {
      results.push(responses[j]);
    }
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

// ── Preparação (usada só por 00-write-seed.js) ────────────────────────────────
export function seedWallets(n) {
  const branchRes = http.post(`${STAFF_URL}/branches`,
    JSON.stringify({ region: 'Sudeste' }), { headers: HEADERS });
  const branchId = branchRes.json('branchId');

  const amRes = http.post(`${STAFF_URL}/account-managers`,
    JSON.stringify({ name: 'Gerente K6', branchId, isAdmin: true }), { headers: HEADERS });
  const accountManagerId = amRes.json('accountManagerId');
  waitForRead(`${READ_URL}/account-managers/${accountManagerId}`, () => true);

  const customerReqs = [];
  for (let i = 0; i < n; i++) {
    customerReqs.push({
      method: 'POST',
      url: `${CUSTOMER_URL}/customers`,
      body: JSON.stringify({ name: `Cliente Write ${i}`, address: `Rua ${i}, 100`, age: 30 + (i % 50), accountManagerId }),
      params: { headers: HEADERS },
    });
  }
  const customerResps = batchRequests(customerReqs);
  const createdCustomers = customerResps.filter((r) => r.status === 201).length;
  waitForRead(`${READ_URL}/customers/by-account-manager/${accountManagerId}`,
    (r) => r.json().length >= createdCustomers);

  const walletReqs = [];
  for (let i = 0; i < customerResps.length; i++) {
    const customerId = customerResps[i].json('customerId');
    if (!customerId) { console.warn(`[seed] customer ${i} falhou`); continue; }
    walletReqs.push({
      method: 'POST',
      url: `${WALLET_URL}/wallets`,
      body: JSON.stringify({ customerId }),
      params: { headers: HEADERS },
    });
  }
  const walletResps = batchRequests(walletReqs);

  const depositReqs = [];
  const wallets = [];
  for (let i = 0; i < walletResps.length; i++) {
    const walletId = walletResps[i].json('walletId');
    if (!walletId) { console.warn(`[seed] wallet ${i} falhou`); continue; }
    wallets.push(walletId);
    depositReqs.push({
      method: 'POST',
      url: `${WALLET_URL}/wallets/${walletId}/transactions`,
      body: JSON.stringify({ walletId, value: 50000.00, type: 'DEPOSIT', exchangeConfirmationId: randomUUID() }),
      params: { headers: HEADERS },
    });
  }
  batchRequests(depositReqs);

  console.log(`[seed] ${wallets.length}/${n} wallets prontos`);
  return wallets;
}

// ── Degraus da rampa ──────────────────────────────────────────────────────────
function parseSeconds(duration) {
  const m = /^(\d+)(s|m)$/.exec(duration);
  if (!m) throw new Error(`duracao nao suportada: ${duration}`);
  return Number(m[1]) * (m[2] === 'm' ? 60 : 1);
}

function stageWindows(stages) {
  const windows = [];
  let start = 0;
  let previous = 0;
  for (const s of stages) {
    const end = start + parseSeconds(s.duration);
    let name;
    if (s.target === 0)             name = 'descida';
    else if (s.target === previous) name = `patamar-${s.target}`;
    else                            name = `rampa-${s.target}`;
    windows.push({ name, start, end });
    start = end;
    previous = s.target;
  }
  return windows;
}

export function writeOptions(stages) {
  const thresholds = {
    http_req_failed:   ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
  };
  // Submétricas por patamar: só para aparecerem no resumo, não alteram o veredito.
  for (const w of stageWindows(stages)) {
    if (!w.name.startsWith('patamar-')) continue;
    thresholds[`http_req_duration{stage:${w.name}}`] = ['p(95)>=0'];
    thresholds[`http_req_failed{stage:${w.name}}`]   = ['rate>=0'];
    thresholds[`http_reqs{stage:${w.name}}`]         = ['count>=0'];
  }
  return {
    summaryTrendStats: ['min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    scenarios: {
      write_load: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages,
      },
    },
    thresholds,
  };
}

export function stageTagger(stages) {
  const windows = stageWindows(stages);
  return function currentStage() {
    const elapsed = (Date.now() - exec.scenario.startTime) / 1000;
    for (const w of windows) {
      if (elapsed < w.end) return w.name;
    }
    return 'descida';
  };
}

// ── Carga ─────────────────────────────────────────────────────────────────────
export function checkWallets(wallets, n) {
  if (wallets.length < n) {
    throw new Error(`${walletsFile(n)} tem ${wallets.length} carteiras, esperado ${n}. Rode antes: k6 run -e N=${n} 00-write-seed.js`);
  }
}

export function writeTransaction(wallets, stage) {
  const walletId = wallets[(__VU - 1) % wallets.length];
  const type     = __ITER % 3 === 0 ? 'WITHDRAWAL' : 'DEPOSIT';

  const res = http.post(
    `${WALLET_URL}/wallets/${walletId}/transactions`,
    JSON.stringify({ walletId, value: 100.00, type, exchangeConfirmationId: randomUUID() }),
    { headers: HEADERS, tags: { stage } }
  );

  check(res, { 'status 201': (r) => r.status === 201 });
}

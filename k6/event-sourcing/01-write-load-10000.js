/**
 * CQRS Event Sourcing — Write Load Test (10000 VUs)
 *
 * Requer as carteiras criadas antes pela preparação (00-write-seed.js), que roda
 * separada para não misturar as requisições de setup nas métricas da carga.
 * A lógica fica em lib/write.js.
 *
 * Uso (a partir desta pasta):
 *   k6 run -e N=10000 00-write-seed.js
 *   k6 run 01-write-load-10000.js
 */

import { SharedArray } from 'k6/data';
import { writeOptions, stageTagger, walletsFile, checkWallets, writeTransaction } from './lib/write.js';

const N = 10000;

const STAGES = [
  { duration: '30s', target: 3333 },
  { duration: '60s', target: 3333 },
  { duration: '30s', target: 6667 },
  { duration: '60s', target: 6667 },
  { duration: '30s', target: 10000 },
  { duration: '60s', target: 10000 },
  { duration: '30s', target: 0 },
];

export const options = writeOptions(STAGES);

const wallets = new SharedArray('wallets', () => JSON.parse(open(walletsFile(N))));
checkWallets(wallets, N);

const currentStage = stageTagger(STAGES);

export default function () {
  writeTransaction(wallets, currentStage());
}

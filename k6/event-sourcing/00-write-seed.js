/**
 * CQRS Event Sourcing — preparação do teste de escrita
 *
 * Cria N clientes + N carteiras (com depósito inicial de 50.000) e grava os IDs
 * em data/write-wallets-<N>.json, lido depois por 01-write-load-<N>.js.
 * Roda separado da carga para que estas requisições não entrem nas métricas.
 *
 * Uso (a partir desta pasta):
 *   k6 run -e N=10000 00-write-seed.js
 *   k6 run 01-write-load-10000.js
 */

import { seedWallets, walletsFile } from './lib/write.js';

const N = Number(__ENV.N);

export const options = {
  vus: 1,
  iterations: 1,
  setupTimeout: '15m',
};

export function setup() {
  if (!N) throw new Error('informe a quantidade de carteiras: -e N=<VUs>');
  return { wallets: seedWallets(N) };
}

export default function () {}

export function handleSummary(data) {
  const wallets = (data.setup_data && data.setup_data.wallets) || [];
  return {
    [walletsFile(N)]: JSON.stringify(wallets),
    stdout: `[seed] ${wallets.length}/${N} carteiras gravadas em ${walletsFile(N)}\n`,
  };
}

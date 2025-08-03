// js/bayes.js
import { pctToProb, validPct } from './utils.js';

export const clamp01 = (p) => (isFinite(p) ? Math.max(0, Math.min(1, p)) : NaN);

// Total Bayes-faktor fra evidensliste i sannsynligheter [0,1]
export function productBF(evidences) {
  let bf = 1;
  for (const ev of evidences) {
    if (!ev) continue;
    const { peh, penh } = ev;
    if (peh == null || penh == null) continue;
    if (penh === 0 && peh > 0) return Infinity;
    if (peh === 0 && penh > 0) bf *= 0;
    else if (!(penh === 0 && peh === 0)) bf *= (peh / penh);
  }
  return bf;
}

// Posterior fra prior og total BF (odds-form)
export function posteriorFrom(prior, bfTotal) {
  const pH = clamp01(prior);
  const oddsPrior = pH === 1 ? Infinity : (pH === 0 ? 0 : pH / (1 - pH));

  let oddsPost;
  if (isFinite(bfTotal) && isFinite(oddsPrior)) {
    oddsPost = bfTotal * oddsPrior;
  } else if (bfTotal === Infinity && oddsPrior === 0) {
    oddsPost = 0;
  } else if (bfTotal === 0 && oddsPrior === Infinity) {
    oddsPost = 0;
  } else {
    oddsPost = (bfTotal === Infinity) ? Infinity : (bfTotal === 0 ? 0 : NaN);
  }

  if (!isFinite(oddsPost)) return oddsPost === Infinity ? 1 : (oddsPost === 0 ? 0 : NaN);
  return oddsPost / (1 + oddsPost);
}

// Full beregning fra prosentverdier
export function calculatePosterior(priorPct, evidencesPct) {
  if (!validPct(priorPct)) return NaN;

  const pH = pctToProb(priorPct);
  const evs = evidencesPct
    .filter(ev => validPct(ev.pehPct) && validPct(ev.penhPct))
    .map(ev => ({ peh: pctToProb(ev.pehPct), penh: pctToProb(ev.penhPct) }));

  const bf = productBF(evs);
  return posteriorFrom(pH, bf);
}

// Domenebasert feilmelding ved ugyldige inndata
export function validationError(priorPct, evidencesPct) {
  if (!validPct(priorPct)) return 'Apriori må være i intervallet 0–100 %.';
  for (const ev of evidencesPct) {
    if (ev.pehPct === '' || ev.penhPct === '' || ev.pehPct == null || ev.penhPct == null) continue;
    if (!validPct(ev.pehPct) || !validPct(ev.penhPct)) {
      return 'Alle evidensverdier må være i intervallet 0–100 %.';
    }
  }
  return '';
}

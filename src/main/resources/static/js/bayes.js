// js/bayes.js
import { pctToProb, validPct, clampPct } from './utils.js';

// Full beregning fra prosentverdier
export function calculatePosterior(priorPct, evidencesPct) {
  if (!validPct(priorPct)) return NaN;

  if (priorPct === 0) return 0; // Hvis prior er 0, er posterior alltid 0
  if (priorPct === 100) return 1; // Hvis prior er 100, er posterior alltid 1

  const pH = pctToProb(priorPct);
  const evs = evidencesPct
    .filter(ev => validPct(ev.pehPct) && validPct(ev.penhPct))
    .map(ev => ({ peh: pctToProb(ev.pehPct), penh: pctToProb(ev.penhPct), weight: pctToProb(ev.weight) }));

  const bf = productBF(evs);
  return posteriorFrom(pH, bf);
}


// Total Bayes-faktor fra evidensliste i sannsynligheter [0,1]
export function productBF(evidences) {
  let bf = 1;
  for (const ev of evidences) {
    if (!ev) continue;
    const { peh, penh, weight } = ev;
    if (peh == null || penh == null) continue;
    if (peh === 0 && penh === 0) continue; // udefinert
    if (penh === 0 && peh > 0) return Infinity;
    if (peh === 0 && penh > 0) return 0;

    const base = peh / penh;

    const exponent = 2 * weight;                 // 50 → 1, 100 → 2, 0 → 0

    bf *= Math.pow(base, exponent);
  }
  return bf;
}

// Posterior fra prior og total BF (odds-form)
export function posteriorFrom(prior, bfTotal) {
  const pH = prior;
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

// Domenebasert feilmelding ved ugyldige inndata
export function validationError(priorPct, evidencesPct) {
  if (!validPct(priorPct)) return 'Initial "gut feeling" must be from 0 to 100 %';
  for (const ev of evidencesPct) {
    if (ev.pehPct === '' || ev.penhPct === '' || ev.pehPct == null || ev.penhPct == null) continue;
    if (!validPct(ev.pehPct) || !validPct(ev.penhPct)) {
      return 'All evidence percentages must be from 0 to 100 %';
    }
  }
  return '';
}

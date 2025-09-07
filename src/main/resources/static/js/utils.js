// js/utils.js


// Klamper prosentverdi til [0,100]. Tillater komma som desimalskille.
// Forutsetter at ev. ledende . eller , allerede er prefikset med 0 i input-handlere.
export const clampPct = (val) => {
  if (val === '' || val == null) return val;         // la tom streng stå under skriving
  const x = parseFloat(String(val).replace(',', '.'));
  if (isNaN(x))
    return 50;
  return x < 1 ? 1 : x > 100 ? 100 : x;
};

export const validPct = (v) =>
  v !== null && v !== '' && isFinite(v) && v > 0 && v <= 100;

export const pctToProb = (pct) => {
  if (pct === '' || pct == null || isNaN(pct)) return null;
  return pct / 100;
};

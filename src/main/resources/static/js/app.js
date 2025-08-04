// js/app.js
import { calculatePosterior, validationError } from './bayes.js';
import { clampPct } from './utils.js';

const { createApp, reactive, ref, computed, onMounted } = window.Vue;

createApp({
  setup() {
    // JSON-data
    const title = ref('');
    const intro = ref('');
    const showHelp = ref(false);

    function getIdFromUrl() {
      const params = new URLSearchParams(window.location.search);
      return params.get('id');
    }

    onMounted(async () => {
      const id = getIdFromUrl();
      if (!id) {
        title.value = 'Feil';
        intro.value = 'Ingen ID angitt i URL.';
        return;
      }

      try {
        const res = await fetch(`evidence/${id}.json`);
        if (!res.ok) throw new Error('Fant ikke fil');
        const data = await res.json();
        title.value = data.title || '';
        intro.value = data.intro || '';
      } catch (err) {
        title.value = 'Feil';
        intro.value = 'Kunne ikke laste inn data: ' + err.message;
      }
    });


    // UI-state
    const priorPct = ref(50);
    const evidences = reactive([{ id: 1, pehPct: 80, penhPct: 30 }]);
    let nextId = 2;
    const errorMsg = ref('');

    // Utregnet resultat + presentasjon
    const posterior = computed(() => calculatePosterior(priorPct.value, evidences));
    const posteriorPctText = computed(() => {
      const p = posterior.value;
      if (!isFinite(p) || p < 0 || p > 1) return '—';
      return (p * 100).toFixed(2).replace('.', ',') + ' %';
    });

    // Valideringstrigger
    const recalc = () => {
      errorMsg.value = validationError(priorPct.value, evidences);
    };

    // UI-hendelser
    const addEvidence = () => evidences.push({ id: nextId++, pehPct: null, penhPct: null });
    const removeEvidence = (idx) => evidences.splice(idx, 1);
    const resetAll = () => {
      priorPct.value = 50;
      evidences.splice(0, evidences.length, { id: 1, pehPct: 80, penhPct: 30 });
      nextId = 2;
      errorMsg.value = '';
      recalc();
    };

    // Blokker vitenskapelig notasjon og +- i number-felt
    const blockNonNumeric = (e) => {
      if (['e', 'E', '+', '-'].includes(e.key)) e.preventDefault();
    };

    // Prefikserer 0 ved .xxx/ ,xxx, klamper 0–100, oppdaterer state + feltet
    const onPriorInput = (e) => {
      console.log('priorInput', e.target.value);
      let s = String(e.target.value || '').trim();
      if (s.startsWith('.') || s.startsWith(',')) {
        console.log("Prefikserer 0 til priorPct");
        s = '0' + s;
        console.log("Nytt priorPct:", s);
      }

      // Bruk clampPct for å støtte komma og clamp 0–100
      const x = clampPct(s);
      priorPct.value = x;

      // Skriv tilbake til input-feltet, så "0.444" vises umiddelbart
      e.target.value = String(x);

      recalc(); // oppdater feilmeldinger umiddelbart (valgfritt)
    };

    const onEvInput = (e, ev, key) => {
      let s = String(e.target.value || '').trim();
      if (s.startsWith('.') || s.startsWith(',')) s = '0' + s;

      const x = clampPct(s);
      ev[key] = x;
      e.target.value = String(x);

      recalc();
    };



    // Eksponer til template
    return {
      priorPct, evidences, errorMsg,
      posteriorPctText,
      addEvidence, removeEvidence, resetAll, recalc,
      clampPct, blockNonNumeric,
      onPriorInput, onEvInput,
      title, intro, showHelp
    };
  }
}).mount('#app');

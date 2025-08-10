// js/app.js
import {calculatePosterior, validationError} from './bayes.js';
import {clampPct} from './utils.js';

const {createApp, reactive, ref, computed, onMounted} = window.Vue;

createApp({
  setup() {
    // Initialisering
    const hypJson = ref({
      title: '',
      intro: '',
      evidence: []
    });
    const evidences = reactive([]); // start tomt
    const loadedEvidenceData = ref([]); // start tomt, vil fylles med data/evidence fra json
    const showHelp = ref(false);
    const resultsDialog = ref(null);
    const publishJson = ref('');

    function openPublish() {
      // Ta med KUN besvarte evidenser (begge felter utfylt)
      const answered = evidences
        .filter(ev => ev.pehPct !== null && ev.penhPct !== null)
        .map(ev => {
          const label = hypJson.value?.evidence?.[ev.id - 1]?.id ?? `E${ev.id}`;
          return {
            id: ev.id,
            label,
            pehPct: ev.pehPct,
            penhPct: ev.penhPct,
            weight: ev.weight
          };
        });

      const payload = {
        title: hypJson.value?.title ?? '',
        aprioriPct: priorPct.value,
        posteriorPct: Number.isFinite(posterior.value) ? +(posterior.value * 100).toFixed(2) : null,
        evidences: answered
      };

      publishJson.value = JSON.stringify(payload, null, 2);
      resultsDialog.value?.showModal();
    }

    function closePublish() {
      resultsDialog.value?.close();
    }

    async function copyPublish() {
      try {
        await navigator.clipboard.writeText(publishJson.value);
      } catch (_) {
        // no-op; kunne evt. vise en liten melding
      }
    }

    function getIdFromUrl() {
      const params = new URLSearchParams(window.location.search);
      return params.get('id');
    }

    function backgroundClass(pct) {
      if (!isFinite(pct)) return '';
      if (Math.abs(pct - 50) < 0.01) return 'bg-yellow';
      if (pct < 50) return 'bg-red';
      return 'bg-green';
    }

    function backgroundClassEv(evidenceIndex) {
      const totalPct = evidences[evidenceIndex].pehPct - evidences[evidenceIndex].penhPct;
      if (Math.abs(totalPct) < 0.01) return "bg-yellow";
      else if (totalPct < 0) return "bg-red";
      else return "bg-green";
    }


    onMounted(async () => {
      const id = getIdFromUrl();
      const res = await fetch(`evidence/${id}.json`);
      hypJson.value = await res.json();

      // Fjern eksisterende evidensvurderinger
      evidences.splice(0, evidences.length);

      // Lagre alle JSON-evidensobjekter internt
      loadedEvidenceData.value = hypJson.value.evidence;

      // Vis første evidence
      evidences.push({
        id: 1,
        pehPct: null,
        penhPct: null,
        weight: 50
      });

      nextId = 2;
    });

    // UI-state
    const priorPct = ref(50);
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

    const removeEvidence = (idx) => {
      evidences.splice(idx, 1);

      // Hvis man fjernet den siste synlige evidensen
      if (idx === evidences.length && nextId > 1) {
        nextId--;
      }

      recalc();
    };


    const resetAll = () => {
      priorPct.value = 50;
      evidences.splice(0, evidences.length, {id: 1, pehPct: 50, penhPct: 50, weight: 50});
      nextId = 2;
      errorMsg.value = '';
      recalc();
    };

    // Blokker vitenskapelig notasjon og +- i number-felt
    const blockNonNumeric = (e) => {
      if (['e', 'E', '+', '-', '.', ','].includes(e.key)) e.preventDefault();
    };

    // Prefikserer 0 ved .xxx/ ,xxx, klamper 0–100, oppdaterer state + feltet
    const onPriorInput = (e) => {
      let s = String(e.target.value || '').trim();
      if (s.startsWith('.') || s.startsWith(',')) {
        s = '0' + s;
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
      const x = clampPct(s);
      ev[key] = x;
      e.target.value = String(x);

      recalc();
    };

    // Add new evidence from button click
    const addEvidence = () =>
    {
      // Do not add if nextId exceeds loaded evidence data length
      if (nextId > hypJson.value.evidence.length) {
        errorMsg.value = 'All evidences are shown';
        return;
      }
      evidences.push({id: nextId++, pehPct: null, penhPct: null, weight: 50});
    }

    // Add new evidence through submitting the evidence form (both fields)
    const checkAutoAppendEvidence = () => {
      const lastIndex = evidences.length - 1;
      const lastEv = evidences[lastIndex];

      if (
        lastEv &&
        lastEv.pehPct !== null &&
        lastEv.penhPct !== null &&
        loadedEvidenceData.value.length > 0 &&
        evidences.length < loadedEvidenceData.value.length
      ) {
        evidences.push({
          id: nextId++,
          pehPct: null,
          penhPct: null,
          weight: 50
        });
      }
    };

    function extractUrl(ref) {
      const match = ref.match(/https?:\/\/\S+/);
      return match ? match[0] : '#';
    }


    // Eksponer til template
    return {
      priorPct, evidences, errorMsg,
      posterior, posteriorPctText,
      addEvidence, removeEvidence, resetAll, recalc,
      clampPct, blockNonNumeric,
      onPriorInput, onEvInput,
      hypJson, showHelp, backgroundClass, backgroundClassEv,
      checkAutoAppendEvidence, extractUrl,
      openPublish, closePublish, copyPublish, publishJson, resultsDialog
    };
  }
}).mount('#app');

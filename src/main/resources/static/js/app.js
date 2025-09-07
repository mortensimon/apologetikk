// js/app.js
import {calculatePosterior, validationError} from './bayes.js';
import {clampPct} from './utils.js';

const {createApp, reactive, ref, computed, onMounted} = window.Vue;

createApp({
  setup() {
    // Initialisering
    const hypJson = ref({
      name: '',
      title: '',
      intro: '',
      evidence: []
    });
    const evidences = reactive([]); // start tomt
    const loadedEvidenceData = ref([]); // start tomt, vil fylles med data/evidence fra json
    const showHelp = ref(false);
    const resultsDialog = ref(null);
    const publishJson = ref('');
    const denomDialog = ref(null);
    const denomination = ref('');
    const denominations = [
      'Catholic', 'Eastern-Orthodox', 'Oriental-Orthodox', 'Lutheran', 'Reformed',
      'Anglican', 'Baptist', 'Methodist', 'Evangelical',
      'Non-denominational', 'Adventist', 'JW', 'LDS',
      'Jewish', 'Muslim', 'Hindu', 'Buddhist', 'New-Age',
      'Spiritual', 'Agnostic', 'Atheist', 'Other'
    ];

    /****************************************************
     * Logikk knyttet til å publisere/dele resultater
     ****************************************************/


    function startPublish() {
      denomination.value = '';
      denomDialog.value?.showModal();
    }

    function cancelDenomination() {
      denomDialog.value?.close();
    }

    function confirmDenomination() {
      denomDialog.value?.close();
      openPublish(); // reuse your existing builder
    }

    function openPublish() {
      const answered = evidences.filter(ev => (ev.pehPct !== null && ev.penhPct !== null && ev.weight > 0) || ev.weight === 0);

      const payload = {
        name: hypJson.value?.name ?? '',
        title: hypJson.value?.title ?? '',
        denomination: denomination.value || null,
        aprioriPct: priorPct.value,
        posteriorPct: Number.isFinite(posterior.value) ? +(posterior.value * 100).toFixed(2) : null,
        evidence: answered
      };

      publishJson.value = payload;
      resultsDialog.value?.showModal();
    }

    function fmtPct(v) {
      if (v == null || isNaN(v)) return '—';
      return `${(+v).toFixed(2)}%`;
    }

    function closePublish() {
      resultsDialog.value?.close();
    }

    async function copyPublish() {
      try {
        const text = JSON.stringify(publishJson.value, null, 2);
        await navigator.clipboard.writeText(text);
      } catch (_) {
        // no-op; kunne evt. vise en liten melding
      }
    }

    async function publishToServer() {
      try {
        const res = await fetch('/api/results', {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify(publishJson.value)
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok) {
          alert(`Failed to publish: ${data.message || res.statusText}`);
          return;
        }
        alert('Results published successfully. File: ' + (data.href || 'n/a'));
      } catch (e) {
        alert('Failed to publish: ' + (e && e.message ? e.message : String(e)));
      }
    }


    /****************************************************
     * Logikk knyttet til å hente ut individuelle resultater
     ****************************************************/



    async function openView() {
      try {
        const input = prompt('Paste result link or id (UUID):');
        if (!input) return;
        const uuid = (input.trim().split('/').filter(Boolean).pop() || '').trim();
        if (!uuid) return alert('No id provided');
        const res = await fetch(`/api/results/${encodeURIComponent(uuid)}`);
        const data = await res.json().catch(() => null);
        if (!res.ok) {
          const msg = (data && data.message) ? data.message : res.statusText;
          return alert('Failed to load: ' + msg);
        }
        publishJson.value = data;
        resultsDialog.value?.showModal();
      } catch (e) {
        alert('Failed to load: ' + (e && e.message ? e.message : String(e)));
      }
    }

    /****************************************************
     * Logikk knyttet til å farger på rader i tabeller
     ****************************************************/

    // Brukes i hovedtabellen
    function backgroundClass(pct) {
      if (!isFinite(pct)) return '';
      if (Math.abs(pct - 50) < 0.01) return 'bg-yellow';
      if (pct < 50) return 'bg-red';
      return 'bg-green';
    }

    // Brukes i publiser-dialogen (man viser tabellen med alle evidensene der)
    function backgroundClassEv(evidenceIndex) {
      const totalPct = evidences[evidenceIndex].pehPct - evidences[evidenceIndex].penhPct;
      if (Math.abs(totalPct) < 0.01) return "bg-yellow";
      else if (totalPct < 0) return "bg-red";
      else return "bg-green";
    }


    /****************************************************
     * Logikk knyttet til oppstart av bayes.html-siden
     ****************************************************/

    function getHypoteseStrFromUrl() {
      const params = new URLSearchParams(window.location.search);
      return params.get('id'); // "id" ble brukt som parameternavn, vi burde vel endre det til "hypotese", men ikke nå (6.9.2025)
    }

    onMounted(async () => {
      const hypoteseStr = getHypoteseStrFromUrl();
      const res = await fetch(`evidence/${hypoteseStr}.json`);
      const json = await res.json();
      // Ensure the name from the query string is preserved on the loaded JSON
      hypJson.value = {...json, name: hypoteseStr};

      // Fjern eksisterende evidensvurderinger
      evidences.splice(0, evidences.length);

      // Lagre alle JSON-evidensobjekter internt
      loadedEvidenceData.value = hypJson.value.evidence;

      // Vis første evidence
      let first = loadedEvidenceData.value[0];
      evidences.push({
        id: first.id,
        head: first.head,
        pehPct: null,
        penhPct: null,
        weight: 50
      });

      nextIdx = 2;
    });


    /****************************************************
     * Logikk knyttet til å beregne posterior
     ****************************************************/

    const priorPct = ref(50);
    const posterior = computed(() => calculatePosterior(priorPct.value, evidences));
    const posteriorPctText = computed(() => {
      const p = posterior.value;
      if (!isFinite(p) || p < 0 || p > 1) return '—';
      return (p * 100).toFixed(2).replace('.', ',') + ' %';
    });


    /****************************************************
     * Logikk knyttet til å håndtere feil
     ****************************************************/

    const errorMsg = ref('');
    const recalc = () => {
      errorMsg.value = validationError(priorPct.value, evidences);
    };






    let nextIdx = 2;

    const removeEvidence = (idx) => {
      evidences.splice(idx, 1);
      // Hvis man fjernet den siste synlige evidensen
      if (idx === evidences.length && nextIdx > 1) {
        nextIdx--;
      }
      recalc();
    };

    const resetAll = () => {
      priorPct.value = 50;
      // Keep only the first evidence, reset its fields
      evidences.splice(0, evidences.length);
      evidences.push({
        id: loadedEvidenceData.value[0].id,
        head: loadedEvidenceData.value[0].head,
        pehPct: null,
        penhPct: null,
        weight: 50
      });
      nextIdx = 2;
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
    const addEvidence = () => {
      // Use loadedEvidenceData as source of truth
      const totalLoaded = loadedEvidenceData.value.length || 0;
      if (evidences.length >= totalLoaded) {
        errorMsg.value = 'All evidences are shown';
        return;
      }

      // Use head from loaded data if available
      const head = loadedEvidenceData.value[nextIdx - 1]?.head ?? null;

      evidences.push({
        id: nextIdx++,
        head,
        pehPct: null,
        penhPct: null,
        weight: 50
      });

      errorMsg.value = '';
      recalc();
    }

    // Add new evidence through submitting the evidence form (both fields)
    const checkAutoAppendEvidence = () => {
      const lastIndex = evidences.length - 1;
      const lastEv = evidences[lastIndex];

      const totalLoaded = loadedEvidenceData.value.length || 0;

      if (
        lastEv &&
        ((lastEv.pehPct !== null && lastEv.penhPct !== null) || lastEv.weight === 0) &&
        totalLoaded > 0 &&
        evidences.length < totalLoaded
      ) {
        const head = loadedEvidenceData.value[nextIdx - 1]?.head ?? null;
        evidences.push({
          id: nextIdx++,
          head,
          pehPct: null,
          penhPct: null,
          weight: 50
        });
        errorMsg.value = '';
        recalc();
      }
    };

    function extractUrl(ref) {
      const match = ref.match(/https?:\/\/\S+/);
      return match ? match[0] : '#';
    }

    function makeRefIntoTextWithLink(ref) {
      if (!ref) return '';
      const url = extractUrl(ref);
      if (url === '#') return ref; // no URL found, return original text
      return ref.replace(url, `<a href="${url}" target="_blank" rel="noopener noreferrer">${url}</a>`);
    }

    function openUrl(url) {
      window.location.href = url;
    }

    function openUrlNewTab(url) {
      window.open(url, '_blank', 'noopener,noreferrer');
    }


    // Eksponer til template
    return {
      priorPct, evidences, errorMsg,
      posterior, posteriorPctText,
      addEvidence, removeEvidence, resetAll, recalc,
      clampPct, blockNonNumeric,
      onPriorInput, onEvInput,
      hypJson, showHelp, backgroundClass, backgroundClassEv,
      checkAutoAppendEvidence, extractUrl, makeRefIntoTextWithLink,
      openPublish, closePublish, copyPublish, publishToServer, publishJson, resultsDialog,
      fmtPct, startPublish, cancelDenomination, confirmDenomination,
      denomination, denominations, denomDialog, openView, openUrl,openUrlNewTab
    };
  }
}).mount('#app');

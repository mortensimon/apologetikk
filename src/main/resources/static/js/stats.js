// stats.js – egen modul for stats-visning (ingen avhengighet til bayes.js)
// Bruker samme Vue-global som app.js (window.Vue)

const { createApp, ref, computed, onMounted } = window.Vue;

createApp({
  setup() {
    const hypSlug = ref(null);
    const hypJson = ref(null);
    const loading = ref(true);
    const error = ref(null);

    const aprioriPct = computed(() => {
      if (!hypJson.value) return null;
      // Støtt både aprioriPct (0–100) og apriori (0–1)
      if (Number.isFinite(hypJson.value.aprioriPct)) return hypJson.value.aprioriPct;
      if (Number.isFinite(hypJson.value.apriori))    return hypJson.value.apriori * 100;
      return null;
    });

    const posteriorPct = computed(() => {
      if (!hypJson.value) return null;
      if (Number.isFinite(hypJson.value.posteriorPct)) return hypJson.value.posteriorPct;
      if (Number.isFinite(hypJson.value.posterior))    return hypJson.value.posterior * 100;
      return null;
    });

    const allReferences = computed(() => {
      if (!hypJson.value?.evidence) return [];
      const refs = [];
      for (const ev of hypJson.value.evidence) {
        if (Array.isArray(ev.references)) refs.push(...ev.references);
      }
      return [...new Set(refs)];
    });
    const hasAnyReferences = computed(() => allReferences.value.length > 0);

    function fmtPct(v) {
      if (v == null || !isFinite(v)) return '—';
      return `${(+v).toFixed(2).replace('.', ',')} %`;
    }

    // Samme farge-logikk som i app.js (nøytral rundt 50 %)
    function backgroundClass(pct) {
      if (!isFinite(pct)) return '';
      if (Math.abs(pct - 50) < 0.01) return 'bg-yellow';
      if (pct < 50) return 'bg-red';
      return 'bg-green';
    }

    // Gjenbruk av radfarge per evidens: pehPct - penhPct (som i app.js)
    function backgroundClassEv(evidenceIndex) {
      const ev = hypJson.value?.evidence?.[evidenceIndex];
      if (!ev) return '';
      const peh = toPct(ev.pehPct);
      const penh = toPct(ev.penhPct);
      const totalPct = peh - penh;
      if (Math.abs(totalPct) < 0.01) return 'bg-yellow';
      else if (totalPct < 0) return 'bg-red';
      else return 'bg-green';
    }

    function toPct(v) {
      if (v == null || !isFinite(+v)) return 0;
      const n = +v;
      // Hvis input ser ut til å være 0–1, skaler til 0–100
      return n <= 1 ? n * 100 : n;
    }

    function extractUrl(ref) {
      const m = String(ref || '').match(/https?:\/\/\S+/);
      return m ? m[0] : '#';
    }

    async function copyJson() {
      try {
        await navigator.clipboard.writeText(JSON.stringify(hypJson.value, null, 2));
        alert('Kopiert!');
      } catch {
        alert('Kunne ikke kopiere.');
      }
    }

    async function load() {
      try {
        const params = new URLSearchParams(location.search);
        hypSlug.value = params.get('hyp') || params.get('h');
        if (!hypSlug.value) {
          error.value = 'Mangler ?hyp=… i URL.';
          return;
        }
        const url = `./data/${hypSlug.value}/average.json`;
        const res = await fetch(url, { cache: 'no-store' });
        if (!res.ok) throw new Error(`Kunne ikke hente ${url} (${res.status})`);
        const json = await res.json();

        // Anta samme struktur som hypJson i app.js. Hvis felter avviker, kan det mappes her.
        hypJson.value = json;
      } catch (e) {
        console.error(e);
        error.value = e?.message || 'Ukjent feil ved lasting.';
      } finally {
        loading.value = false;
      }
    }

    onMounted(load);

    return {
      // state
      hypSlug, hypJson, loading, error,
      // computed
      aprioriPct, posteriorPct, allReferences, hasAnyReferences,
      // methods
      fmtPct, backgroundClass, backgroundClassEv, extractUrl, copyJson
    };
  }
}).mount('#app');

// stats.js – egen modul for stats-visning (ingen avhengighet til bayes.js)
// Bruker samme Vue-global som app.js (window.Vue)

const { createApp, ref, computed, onMounted } = window.Vue;

createApp({
  template: `
    <template v-if="loading">
      <p class="lead">Laster …</p>
    </template>

    <template v-else-if="error">
      <div class="err">{{ error }}</div>
      <p class="lead">
        Åpne som <code>stats.html?hyp=&lt;navn&gt;</code>.<br>
        Eksempel: <code>stats.html?hyp=papacy</code> → <code>data/papacy/average.json</code>.
      </p>
    </template>

    <template v-else-if="hypJson">
      <h1>{{ hypJson.title }}</h1>
      <p class="lead">{{ hypJson.intro }}</p>

      <div class="card heading">
        <div class="row-fixed">
          <div>
            <label>Gjennomsnittlig "gut feeling"</label>
            <div class="posterior" :class="backgroundClass(aprioriPct)">{{ fmtPct(aprioriPct) }}</div>
          </div>
          <div>
            <label>Resultat (posterior)</label>
            <div class="posterior" :class="backgroundClass(posteriorPct)">{{ fmtPct(posteriorPct) }}</div>
          </div>
          <div class="right">
            <button class="btn" @click="copyJson">Kopier JSON</button>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="row" style="justify-content:space-between; margin-bottom:6px">
          <strong>Evidences ({{ hypJson.evidence?.length ?? 0 }})</strong>
          <span class="hint">Kilde: data/{{ hypSlug }}/average.json</span>
        </div>

        <table class="table">
          <thead>
          <tr>
            <th>Evidence</th>
            <th>P(E|H)</th>
            <th>P(E|¬H)</th>
            <th>Weight</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(ev, idx) in hypJson.evidence" :key="ev.id ?? idx" :class="backgroundClassEv(idx)">
            <td>{{ ev.label || ev.short || ('E' + (ev.id ?? (idx+1))) }}</td>
            <td class="num">{{ fmtPct(ev.pehPct) }}</td>
            <td class="num">{{ fmtPct(ev.penhPct) }}</td>
            <td class="num">{{ ev.weight }}</td>
          </tr>
          </tbody>
        </table>
      </div>

      <details class="details-block" v-if="hasAnyReferences" style="margin-top:10px;">
        <summary class="btn small">Vis alle referanser</summary>
        <ul style="margin-top:8px;">
          <li v-for="(ref, i) in allReferences" :key="'ref-'+i">
            <a :href="extractUrl(ref)" target="_blank" rel="noopener">{{ ref }}</a>
          </li>
        </ul>
      </details>
    </template>
  `,
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

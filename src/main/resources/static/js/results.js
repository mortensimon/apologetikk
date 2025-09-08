// results.js
const {createApp} = Vue;


function getQueryParams() {
  const params = {};
  window.location.search.replace(/^\?/, '').split('&').forEach(pair => {
    const [key, value] = pair.split('=');
    if (key) params[key] = decodeURIComponent(value || '');
  });
  return params;
}

createApp({
  data() {
    return {
      name: '',
      id: '',
      publishJson: null,
      error: '',
    };
  },
  computed: {
    testLink() {
      // Link to bayes.html with same hypothesis
      return `./bayes.html?id=${encodeURIComponent(this.name)}`;
    }
  },
  methods: {
    fmtPct(val) {
      if (val === undefined || val === null) return '';
      return (val).toFixed(1) + '%';
    },
    // Brukes i publiser-dialogen (man viser tabellen med alle evidensene der)
    backgroundClassEv(evidenceIndex) {
      if (!this.publishJson || !this.publishJson.evidence) return '';
      const evidences = this.publishJson.evidence;
      if (evidenceIndex < 0 || evidenceIndex >= evidences.length) return '';
      if (evidences[evidenceIndex].pehPct == null || evidences[evidenceIndex].penhPct == null)
        return '';
      // Grønn bakgrunn hvis positiv effekt, rød hvis negativ, gul hvis nøytral
      const totalPct = evidences[evidenceIndex].pehPct - evidences[evidenceIndex].penhPct;
      if (Math.abs(totalPct) < 0.01) return "bg-yellow";
      else if (totalPct < 0) return "bg-red";
      else return "bg-green";
    }
  },
  mounted() {
    const params = getQueryParams();
    this.name = params.name || '';
    this.id = params.id || '';
    if (!this.name || !this.id) {
      this.error = 'Missing hypothesis name or ID in URL.';
      return;
    }
    // Fetch result data from REST API
    fetch(`/api/results/${encodeURIComponent(this.id)}`)
      .then(resp => {
        if (!resp.ok) throw new Error('Failed to fetch results');
        return resp.json();
      })
      .then(json => {
        this.publishJson = json;
      })
      .catch(err => {
        this.error = 'Could not load results: ' + err.message;
      });
  }
}).mount('#results-js');


const {createApp, reactive, onMounted} = window.Vue;

createApp({
  setup() {
    const stats = reactive({
        averages: [],
        calcBgColor(ev) {
          if (!ev) return '';
          if (ev.countDisregardPct >= 50) return 'gray';
          if (ev.pehPct > ev.penhPct) {
            const diffPct = ev.pehPct - ev.penhPct;
            const diff = diffPct * (ev.weight*2) / 100;
            if (diff < 5)
              return 'green-1';
            if (diff < 20)
              return 'green-2';
            if (diff < 50)
              return 'green-3';
            if (diff < 80)
              return 'green-4';
            return 'green-5';
          }
          if (ev.pehPct < ev.penhPct) {
            const diff = ev.penhPct - ev.pehPct;
            if (diff < 5)
              return 'red-1';
            if (diff < 20)
              return 'red-2';
            if (diff < 50)
              return 'red-3';
            if (diff < 80)
              return 'red-4';
            return 'red-5';
          }
          return 'yellow';
        }
      })
    ;

    async function load() {
      const params = new URLSearchParams(location.search);
      const hyp = params.get('hyp');
      const url = `./api/average?hypothesis=${hyp}`;
      const res = await fetch(url)
      const data = await res.json();
      stats.averages = Array.isArray(data.averages) ? data.averages : [];
      console.log('Averages loaded (#)', stats.averages.length);
      console.log("URL:", url);
      console.log("Data.length:", data);
    }

    onMounted(load);

    return {stats};
  }
}).mount('#stats-js');

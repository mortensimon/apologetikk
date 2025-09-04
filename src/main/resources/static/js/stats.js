const {createApp, reactive, onMounted} = window.Vue;

createApp({
  setup() {
    const stats = reactive({
        averages: [],
        calcBgColor(ev) {
          if (!ev) return '';
          if (ev.weight === 0) return 'gray';
          if (ev.pehPct > ev.penhPct) {
            const diff = ev.pehPct - ev.penhPct;
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
      console.log(data.averages);
    }

    onMounted(load);

    return {stats};
  }
}).mount('#stats-js');

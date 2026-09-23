/**
 * ECharts UI for monetization calculator.
 * - Preserves user zoom across data updates
 * - Axis tooltips with exact values
 * - Zoom presets: до плато / 90д / год
 */
(function (root) {
  const M = () => root.AliceMonetization;

  /** Per-chart interaction state (weak map by chart instance). */
  const chartState = new WeakMap();

  function getState(chart) {
    if (!chartState.has(chart)) {
      chartState.set(chart, {
        ready: false,
        zoom: null, // { start, end } percent 0–100
        bound: false,
      });
    }
    return chartState.get(chart);
  }

  function daysAxis(days) {
    const labels = [];
    for (let t = 0; t <= days; t++) labels.push(t === 0 ? 'сегодня' : String(t));
    return labels;
  }

  function seriesFromDay0(arr, days) {
    const out = [];
    for (let t = 0; t <= days; t++) out.push(Math.round((arr[t] || 0) * 100) / 100);
    return out;
  }

  function defaultZoomEndPercent(sim) {
    const days = sim.days || 365;
    const endDay = sim.dynamicsEndDay != null ? sim.dynamicsEndDay : Math.min(60, days);
    return Math.max(100 * (21 / days), Math.min(100, (endDay / days) * 100));
  }

  function zoomForPreset(sim, preset) {
    const days = sim.days || 365;
    if (preset === 'year') return { start: 0, end: 100 };
    if (preset === '90') return { start: 0, end: Math.min(100, (90 / days) * 100) };
    // plateau / dynamics
    return { start: 0, end: defaultZoomEndPercent(sim) };
  }

  function readZoom(chart) {
    try {
      const opt = chart.getOption();
      const dz = opt && opt.dataZoom && opt.dataZoom[0];
      if (dz && typeof dz.start === 'number' && typeof dz.end === 'number') {
        return { start: dz.start, end: dz.end };
      }
    } catch (_) {}
    return null;
  }

  function bindZoomPersist(chart, kind) {
    const st = getState(chart);
    if (st.bound) return;
    st.bound = true;
    st.kind = kind || st.kind;
    chart.on('datazoom', () => {
      const z = readZoom(chart);
      if (z) st.zoom = z;
      if (st.kind === 'finance' && st.lastSim && st.lastMode) {
        rescaleFinanceY(chart, st);
      }
    });
    chart.on('restore', () => {
      st.zoom = null;
      st.ready = false;
    });
  }

  function rescaleFinanceY(chart, st) {
    const sim = st.lastSim;
    const mode = st.lastMode;
    if (!sim || !M()) return;
    const days = sim.days;
    let data;
    if (mode === 'rolling30') data = seriesFromDay0(sim.rolling30, days);
    else if (mode === 'cumulative') data = seriesFromDay0(sim.cumulative, days);
    else data = seriesFromDay0(sim.dailyRevenue, days);
    const z = st.zoom || { start: 0, end: 100 };
    const i0 = Math.floor((z.start / 100) * days);
    const i1 = Math.ceil((z.end / 100) * days);
    const dataForScale = data.slice(Math.max(0, i0), Math.min(data.length, i1 + 1));
    const markers = (M().CONFIG && M().CONFIG.CUMULATIVE_MARKERS) || [];
    const yDom = M().financeYDomain(
      dataForScale.length ? dataForScale : data.slice(0, (sim.dynamicsEndDay || 60) + 1),
      mode,
      mode === 'cumulative' ? markers : [],
    );
    try {
      chart.setOption({ yAxis: { min: yDom.yMin, max: yDom.yMax } }, { lazyUpdate: true });
    } catch (_) {}
  }

  /** Keep audience ↔ finance zoom in sync. */
  let syncLock = false;
  function linkChartsZoom(audience, finance) {
    if (!audience || !finance) return;
    const sync = (from, to) => {
      from.on('datazoom', () => {
        if (syncLock) return;
        const z = readZoom(from);
        if (!z) return;
        syncLock = true;
        try {
          getState(to).zoom = z;
          to.dispatchAction({ type: 'dataZoom', dataZoomIndex: 0, start: z.start, end: z.end });
          to.dispatchAction({ type: 'dataZoom', dataZoomIndex: 1, start: z.start, end: z.end });
        } catch (_) {
        } finally {
          syncLock = false;
        }
      });
    };
    if (!audience.__mcZoomLinked) {
      audience.__mcZoomLinked = true;
      sync(audience, finance);
    }
    if (!finance.__mcZoomLinked) {
      finance.__mcZoomLinked = true;
      sync(finance, audience);
    }
  }

  function dataZoomOption(zoom) {
    const start = zoom ? zoom.start : 0;
    const end = zoom ? zoom.end : 100;
    return [
      {
        id: 'dz-inside',
        type: 'inside',
        xAxisIndex: 0,
        filterMode: 'none',
        zoomOnMouseWheel: true,
        moveOnMouseMove: true,
        moveOnMouseWheel: false,
        start,
        end,
      },
      {
        id: 'dz-slider',
        type: 'slider',
        xAxisIndex: 0,
        height: 22,
        bottom: 6,
        brushSelect: false,
        showDetail: true,
        start,
        end,
        // Do NOT use custom labelFormatter — it breaks category labels (garbage numbers).
      },
    ];
  }

  function initAudienceChart(dom) {
    if (!root.echarts || !dom) return null;
    const chart = root.echarts.init(dom, null, { renderer: 'canvas' });
    bindZoomPersist(chart, 'audience');
    return chart;
  }

  function initFinanceChart(dom) {
    if (!root.echarts || !dom) return null;
    const chart = root.echarts.init(dom, null, { renderer: 'canvas' });
    bindZoomPersist(chart, 'finance');
    return chart;
  }

  function formatDayLabel(label) {
    if (label === 'сегодня' || label === 0 || label === '0') return 'сегодня (день 0)';
    return `день ${label}`;
  }

  function audienceTooltipFormatter(params) {
    if (!params || !params.length) return '';
    const day = formatDayLabel(params[0].axisValueLabel ?? params[0].axisValue);
    const lines = [`<div style="font-weight:600;margin-bottom:4px">${day}</div>`];
    for (const p of params) {
      if (p.value == null || p.value === '' || (Array.isArray(p.value) && p.value.length === 0)) continue;
      const v = typeof p.value === 'number' ? Math.round(p.value).toLocaleString('ru-RU') : p.value;
      lines.push(
        `<div style="display:flex;gap:8px;align-items:center">` +
          `<span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color}"></span>` +
          `<span>${p.seriesName}</span>` +
          `<span style="margin-left:auto;font-variant-numeric:tabular-nums">${v}</span>` +
          `</div>`,
      );
    }
    return lines.join('');
  }

  function financeTooltipFormatter(params) {
    if (!params || !params.length) return '';
    const day = formatDayLabel(params[0].axisValueLabel ?? params[0].axisValue);
    const lines = [`<div style="font-weight:600;margin-bottom:4px">${day}</div>`];
    for (const p of params) {
      if (p.value == null || p.value === '') continue;
      const raw = typeof p.value === 'number' ? p.value : Number(p.value);
      const v = Number.isFinite(raw)
        ? `${Math.round(raw).toLocaleString('ru-RU')} ₽`
        : p.value;
      lines.push(
        `<div style="display:flex;gap:8px;align-items:center">` +
          `<span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${p.color}"></span>` +
          `<span>${p.seriesName}</span>` +
          `<span style="margin-left:auto;font-variant-numeric:tabular-nums">${v}</span>` +
          `</div>`,
      );
    }
    return lines.join('');
  }

  /**
   * @param {object} opts
   * @param {boolean} [opts.animate]
   * @param {boolean} [opts.resetZoom] force default dynamics zoom
   * @param {'plateau'|'90'|'year'|null} [opts.zoomPreset]
   */
  function updateAudienceChart(chart, sim, opts) {
    if (!chart || !sim || !M()) return;
    const options = typeof opts === 'boolean' ? { animate: opts } : opts || {};
    const st = getState(chart);
    bindZoomPersist(chart, 'audience');

    const days = sim.days;
    const x = daysAxis(days);
    const dau = seriesFromDay0(sim.dau, days);
    const plateau = dau.map(() => Math.round(sim.plateauDau * 100) / 100);
    const yDom = M().audienceYDomain(sim);
    const reqVal = Math.round(sim.requiredDau * 100) / 100;
    const requiredSeries = dau.map(() => reqVal);

    if (options.zoomPreset) {
      st.zoom = zoomForPreset(sim, options.zoomPreset);
    } else if (options.resetZoom || !st.ready) {
      st.zoom = zoomForPreset(sim, 'plateau');
    } else if (!st.zoom) {
      st.zoom = readZoom(chart) || zoomForPreset(sim, 'plateau');
    }

    const markPoint =
      sim.goalDay != null
        ? {
            data: [
              {
                name: 'Цель',
                coord: [
                  sim.goalDay === 0 ? 'сегодня' : String(sim.goalDay),
                  Math.round(sim.dau[sim.goalDay] * 100) / 100,
                ],
                itemStyle: { color: '#2e7d32' },
                label: {
                  formatter:
                    sim.goalDay === 0
                      ? 'Цель уже сегодня!'
                      : `Цель на ${sim.goalDay} день`,
                  color: '#1b5e20',
                  fontWeight: 600,
                  position: 'top',
                },
                symbol: 'pin',
                symbolSize: 42,
              },
            ],
          }
        : { data: [] };

    const series = [
      {
        name: 'DAU',
        type: 'line',
        showSymbol: false,
        symbol: 'circle',
        symbolSize: 6,
        data: dau,
        markPoint,
        z: 3,
        lineStyle: { width: 2.5, color: '#1565c0' },
        itemStyle: { color: '#1565c0' },
        emphasis: { focus: 'series' },
      },
      {
        name: 'Потолок DAU',
        type: 'line',
        showSymbol: false,
        data: plateau,
        z: 2,
        lineStyle: { width: 1.5, color: '#64748b' },
        itemStyle: { color: '#64748b' },
      },
    ];

    if (yDom.includeRequiredInScale) {
      series.push({
        name: 'Требуемый DAU',
        type: 'line',
        showSymbol: false,
        data: requiredSeries,
        z: 1,
        lineStyle: { width: 1.5, type: 'dashed', color: '#c62828' },
        itemStyle: { color: '#c62828' },
      });
      series[0].markLine = { data: [] };
    } else if (Number.isFinite(sim.requiredDau)) {
      series.push({
        name: 'Требуемый DAU',
        type: 'line',
        showSymbol: false,
        data: [],
        lineStyle: { width: 1.5, type: 'dashed', color: '#c62828' },
        itemStyle: { color: '#c62828' },
      });
      series[0].markLine = {
        symbol: 'none',
        data: [
          {
            yAxis: yDom.yMax,
            label: {
              formatter: `Требуемый DAU ${Math.round(sim.requiredDau).toLocaleString('ru-RU')} ↑`,
              position: 'insideEndTop',
              color: '#c62828',
            },
            lineStyle: { type: 'dashed', color: '#c62828', width: 1.5 },
          },
        ],
      };
    }

    chart.setOption(
      {
        animation: !!options.animate,
        animationDuration: 200,
        toolbox: {
          right: 8,
          top: 0,
          feature: {
            dataZoom: { yAxisIndex: 'none', title: { zoom: 'Выделить область', back: 'Назад' } },
            restore: { title: 'Сброс вида' },
          },
        },
        title: {
          show: true,
          text: 'Наведите на линию — точные значения. Колесо/ползунок — масштаб.',
          left: 0,
          top: 28,
          textStyle: { fontSize: 11, fontWeight: 'normal', color: '#64748b' },
        },
        tooltip: {
          trigger: 'axis',
          confine: true,
          enterable: false,
          axisPointer: {
            type: 'cross',
            snap: true,
            label: { backgroundColor: '#1e293b' },
          },
          backgroundColor: 'rgba(255,255,255,0.96)',
          borderColor: '#e2e8f0',
          borderWidth: 1,
          textStyle: { color: '#0f172a', fontSize: 12 },
          formatter: audienceTooltipFormatter,
        },
        legend: {
          data: ['DAU', 'Потолок DAU', 'Требуемый DAU'],
          top: 0,
          left: 0,
        },
        grid: { left: 58, right: 36, top: 72, bottom: 64 },
        dataZoom: dataZoomOption(st.zoom),
        xAxis: {
          type: 'category',
          data: x,
          name: 'День',
          nameLocation: 'middle',
          nameGap: 28,
          boundaryGap: false,
          axisLabel: {
            hideOverlap: true,
            formatter: (v) => (v === 'сегодня' ? 'сегодня' : v),
          },
        },
        yAxis: {
          type: 'value',
          name: 'Пользователи',
          min: yDom.yMin,
          max: yDom.yMax,
          scale: false,
          splitLine: { lineStyle: { color: '#f1f5f9' } },
        },
        series,
      },
      { notMerge: false, lazyUpdate: false, replaceMerge: ['series'] },
    );
    st.ready = true;
  }

  function updateFinanceChart(chart, sim, mode, opts) {
    if (!chart || !sim || !M()) return;
    const options = typeof opts === 'boolean' ? { animate: opts } : opts || {};
    const st = getState(chart);
    bindZoomPersist(chart, 'finance');
    st.lastSim = sim;
    st.lastMode = mode;

    const days = sim.days;
    const x = daysAxis(days);
    let data;
    let name;
    if (mode === 'rolling30') {
      data = seriesFromDay0(sim.rolling30, days);
      name = 'За 30 дней (скользящее)';
    } else if (mode === 'cumulative') {
      data = seriesFromDay0(sim.cumulative, days);
      name = 'Накоплено всего';
    } else {
      data = seriesFromDay0(sim.dailyRevenue, days);
      name = 'За день';
    }

    if (options.zoomPreset) {
      st.zoom = zoomForPreset(sim, options.zoomPreset);
    } else if (options.resetZoom || !st.ready) {
      st.zoom = zoomForPreset(sim, 'plateau');
    } else if (!st.zoom) {
      st.zoom = readZoom(chart) || zoomForPreset(sim, 'plateau');
    }

    // Y-scale from visible zoom window for readability
    const z = st.zoom || { start: 0, end: 100 };
    const i0 = Math.floor((z.start / 100) * days);
    const i1 = Math.ceil((z.end / 100) * days);
    const dataForScale = data.slice(Math.max(0, i0), Math.min(data.length, i1 + 1));
    const markers = (M().CONFIG && M().CONFIG.CUMULATIVE_MARKERS) || [];
    const yDom = M().financeYDomain(
      dataForScale.length ? dataForScale : data.slice(0, (sim.dynamicsEndDay || 60) + 1),
      mode,
      mode === 'cumulative' ? markers : [],
    );

    const markLine =
      mode === 'cumulative' && yDom.visibleMarkers.length
        ? {
            symbol: 'none',
            data: yDom.visibleMarkers.map((v) => ({
              yAxis: v,
              label: { formatter: `${(v / 1000).toLocaleString('ru-RU')} тыс. ₽` },
              lineStyle: { type: 'dotted', color: '#94a3b8' },
            })),
          }
        : { data: [] };

    chart.setOption(
      {
        animation: !!options.animate,
        toolbox: {
          right: 8,
          top: 0,
          feature: {
            dataZoom: { yAxisIndex: 'none', title: { zoom: 'Выделить область', back: 'Назад' } },
            restore: { title: 'Сброс вида' },
          },
        },
        title: {
          show: true,
          text: 'Наведите — точная сумма. Колесо/ползунок — масштаб.',
          left: 0,
          top: 28,
          textStyle: { fontSize: 11, fontWeight: 'normal', color: '#64748b' },
        },
        tooltip: {
          trigger: 'axis',
          confine: true,
          axisPointer: {
            type: 'cross',
            snap: true,
            label: { backgroundColor: '#1e293b' },
          },
          backgroundColor: 'rgba(255,255,255,0.96)',
          borderColor: '#e2e8f0',
          borderWidth: 1,
          textStyle: { color: '#0f172a', fontSize: 12 },
          formatter: financeTooltipFormatter,
        },
        legend: { data: [name], top: 0, left: 0 },
        grid: { left: 72, right: 36, top: 72, bottom: 64 },
        dataZoom: dataZoomOption(st.zoom),
        xAxis: {
          type: 'category',
          data: x,
          name: 'День',
          nameLocation: 'middle',
          nameGap: 28,
          boundaryGap: false,
          axisLabel: { hideOverlap: true },
        },
        yAxis: {
          type: 'value',
          name: '₽',
          min: yDom.yMin,
          max: yDom.yMax,
          scale: false,
          splitLine: { lineStyle: { color: '#f1f5f9' } },
          axisLabel: {
            formatter: (v) =>
              v >= 1000 ? `${Math.round(v / 1000)}k` : String(Math.round(v)),
          },
        },
        series: [
          {
            name,
            type: 'line',
            showSymbol: false,
            symbol: 'circle',
            symbolSize: 6,
            data,
            areaStyle: mode === 'cumulative' ? { opacity: 0.08 } : undefined,
            lineStyle: { width: 2.5, color: '#2e7d32' },
            itemStyle: { color: '#2e7d32' },
            markLine,
            emphasis: { focus: 'series' },
          },
        ],
      },
      { notMerge: false, lazyUpdate: false, replaceMerge: ['series'] },
    );
    st.ready = true;
  }

  function resizeCharts(audience, finance) {
    try {
      audience && audience.resize();
    } catch (_) {}
    try {
      finance && finance.resize();
    } catch (_) {}
  }

  root.AliceMonetization = root.AliceMonetization || {};
  Object.assign(root.AliceMonetization, {
    initAudienceChart,
    initFinanceChart,
    updateAudienceChart,
    updateFinanceChart,
    resizeCharts,
    defaultZoomEndPercent,
    zoomForPreset,
    linkChartsZoom,
  });
})(typeof globalThis !== 'undefined' ? globalThis : window);

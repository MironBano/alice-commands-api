/**
 * Scenario solver — AppMetrica RR curve scale + installs search.
 */
(function (root) {
  const M = root.AliceMonetization;
  const CONFIG = M.CONFIG;

  function hitsGoal(inputs, settings) {
    return M.simulate(inputs, settings).reachableWithinHorizon;
  }

  /**
   * Minimal product: scale RR D1/D7/D14 up by factor until goal reachable.
   */
  function solveMinRetention(baseInputs, settings) {
    const inputs = { ...baseInputs };
    if (hitsGoal(inputs, settings)) {
      return {
        kind: 'min_product',
        feasible: true,
        alreadyOk: true,
        rrScale: 1,
        rrDay1: inputs.rrDay1,
        rrDay7: inputs.rrDay7,
        rrDay14: inputs.rrDay14,
        deltaRetentionPoints: 0,
        label: 'Текущая кривая Rolling Retention уже достаточна',
      };
    }

    // Max scale so rrDay1 hits 100%
    const maxScale = inputs.rrDay1 > 0 ? CONFIG.RR_MAX / inputs.rrDay1 : 10;
    const hiProbe = M.scaleRrInputs(inputs, maxScale);
    if (!hitsGoal(hiProbe, settings)) {
      return {
        kind: 'min_product',
        feasible: false,
        rrScale: null,
        label: 'Даже при RR D1 = 100% цель недостижима за 365 дней при текущих установках и рекламе',
      };
    }

    let lo = 1000; // scale * 1000
    let hi = Math.ceil(maxScale * 1000);
    lo = 1000; // start from 1.0
    // binary search scale in 0.001 steps
    let loI = 1000;
    let hiI = Math.max(1001, Math.ceil(maxScale * 1000));
    while (loI < hiI) {
      const mid = Math.floor((loI + hiI) / 2);
      const ok = hitsGoal(M.scaleRrInputs(inputs, mid / 1000), settings);
      if (ok) hiI = mid;
      else loI = mid + 1;
    }
    const scale = loI / 1000;
    const scaled = M.scaleRrInputs(inputs, scale);
    return {
      kind: 'min_product',
      feasible: true,
      alreadyOk: false,
      rrScale: scale,
      rrDay1: scaled.rrDay1,
      rrDay7: scaled.rrDay7,
      rrDay14: scaled.rrDay14,
      deltaRetentionPoints: M.roundPct(scaled.rrDay1 - inputs.rrDay1),
      label:
        `Поднять кривую RR (×${scale.toFixed(2)}): ` +
        `D1 ${scaled.rrDay1.toFixed(2)}% · D7 ${scaled.rrDay7.toFixed(2)}% · D14 ${scaled.rrDay14.toFixed(2)}%`,
    };
  }

  function solveMinTraffic(baseInputs, settings) {
    const inputs = { ...baseInputs };
    if (hitsGoal(inputs, settings)) {
      return {
        kind: 'min_traffic',
        feasible: true,
        alreadyOk: true,
        installsPerDay: inputs.installsPerDay,
        deltaInstalls: 0,
        label: 'Текущий трафик уже достаточен',
      };
    }

    let lo = inputs.installsPerDay;
    let hi = CONFIG.INSTALLS_PER_DAY_MAX;
    if (!hitsGoal({ ...inputs, installsPerDay: hi }, settings)) {
      return {
        kind: 'min_traffic',
        feasible: false,
        installsPerDay: null,
        deltaInstalls: null,
        label: `Даже при ${hi.toLocaleString('ru-RU')} установок/день цель недостижима за 365 дней`,
      };
    }

    while (lo < hi) {
      const mid = Math.floor((lo + hi) / 2);
      if (hitsGoal({ ...inputs, installsPerDay: mid }, settings)) hi = mid;
      else lo = mid + 1;
    }
    const delta = lo - inputs.installsPerDay;
    return {
      kind: 'min_traffic',
      feasible: true,
      alreadyOk: false,
      installsPerDay: lo,
      deltaInstalls: delta,
      label: `Нужно ${lo.toLocaleString('ru-RU')} установок/день (+${delta.toLocaleString('ru-RU')})`,
    };
  }

  function solveBalance(baseInputs, settings) {
    const inputs = { ...baseInputs };
    const w1 = settings.balanceWeightInstalls ?? CONFIG.DEFAULT_SETTINGS.balanceWeightInstalls;
    const w2 = settings.balanceWeightRetention ?? CONFIG.DEFAULT_SETTINGS.balanceWeightRetention;

    if (hitsGoal(inputs, settings)) {
      return {
        kind: 'balance',
        feasible: true,
        alreadyOk: true,
        installsPerDay: inputs.installsPerDay,
        rrScale: 1,
        rrDay1: inputs.rrDay1,
        rrDay7: inputs.rrDay7,
        rrDay14: inputs.rrDay14,
        deltaInstalls: 0,
        label: 'Цель уже достижима — баланс не нужен',
      };
    }

    const baseI = Math.max(1, inputs.installsPerDay);
    const maxI = CONFIG.INSTALLS_PER_DAY_MAX;
    const maxScale = inputs.rrDay1 > 0 ? CONFIG.RR_MAX / inputs.rrDay1 : 10;

    let best = null;

    function evaluate(scale, i) {
      const scaled = M.scaleRrInputs(inputs, scale);
      const probe = { ...scaled, installsPerDay: i };
      if (!hitsGoal(probe, settings)) return null;
      const dI = i - inputs.installsPerDay;
      const dS = scale - 1;
      const cost = w1 * (dI / baseI) + w2 * (dS / Math.max(0.01, maxScale - 1));
      return {
        kind: 'balance',
        feasible: true,
        alreadyOk: false,
        installsPerDay: i,
        rrScale: scale,
        rrDay1: scaled.rrDay1,
        rrDay7: scaled.rrDay7,
        rrDay14: scaled.rrDay14,
        deltaInstalls: dI,
        installsPct: Math.round((dI / baseI) * 1000) / 10,
        cost,
        label: '',
      };
    }

    function minInstallsForScale(scale) {
      const scaled = M.scaleRrInputs(inputs, scale);
      if (!hitsGoal({ ...scaled, installsPerDay: maxI }, settings)) return null;
      let lo = inputs.installsPerDay;
      let hi = maxI;
      while (lo < hi) {
        const mid = Math.floor((lo + hi) / 2);
        if (hitsGoal({ ...scaled, installsPerDay: mid }, settings)) hi = mid;
        else lo = mid + 1;
      }
      return lo;
    }

    for (let sI = 1000; sI <= Math.ceil(maxScale * 1000); sI += 50) {
      const scale = sI / 1000;
      const i = minInstallsForScale(scale);
      if (i == null) continue;
      const cand = evaluate(scale, i);
      if (cand && (!best || cand.cost < best.cost)) best = cand;
    }

    const minR = solveMinRetention(inputs, settings);
    if (minR.feasible && !minR.alreadyOk && minR.rrScale != null) {
      const cand = evaluate(minR.rrScale, inputs.installsPerDay);
      if (cand && (!best || cand.cost < best.cost)) best = cand;
    }
    const minT = solveMinTraffic(inputs, settings);
    if (minT.feasible && !minT.alreadyOk) {
      const cand = evaluate(1, minT.installsPerDay);
      if (cand && (!best || cand.cost < best.cost)) best = cand;
    }

    if (best) {
      best.label =
        `Компромисс: ${best.installsPerDay.toLocaleString('ru-RU')}/день ` +
        `(${best.installsPct >= 0 ? '+' : ''}${best.installsPct}%), ` +
        `RR ×${best.rrScale.toFixed(2)} (D1 ${best.rrDay1.toFixed(2)}%)`;
      return best;
    }

    return {
      kind: 'balance',
      feasible: false,
      label: 'Не удалось найти компромисс в пределах допустимых диапазонов',
    };
  }

  function solveAllScenarios(baseInputs, settings) {
    const inputs = M.normalizeInputs(baseInputs);
    const sim = M.simulate(inputs, settings);
    if (sim.reachableWithinHorizon) {
      return { needed: false, goalDay: sim.goalDay, scenarios: [] };
    }
    return {
      needed: true,
      goalDay: null,
      scenarios: [
        solveMinRetention(inputs, settings),
        solveMinTraffic(inputs, settings),
        solveBalance(inputs, settings),
      ],
    };
  }

  const api = {
    hitsGoal,
    solveMinRetention,
    solveMinTraffic,
    solveBalance,
    solveAllScenarios,
  };

  root.AliceMonetization = root.AliceMonetization || {};
  Object.assign(root.AliceMonetization, api);

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = api;
  }
})(typeof globalThis !== 'undefined' ? globalThis : window);

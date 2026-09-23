/**
 * Monetization simulation using AppMetrica Rolling Retention.
 *
 * AppMetrica Rolling Retention day N = % of cohort with a session on day N or later.
 * Classic Retention day N = % with a session on exactly day N (needed for DAU).
 *
 * We interpolate RR(d) from D1/D7/D14, then:
 *   activityFactor = classicDay1 / rrDay1
 *   R_active(d) = (RR(d)/100) * activityFactor
 * so DAU follows classic-scale activity, not raw rolling (which overcounts).
 */
(function (root) {
  const CONFIG = root.AliceMonetization.CONFIG;

  function clamp(value, min, max) {
    return Math.min(max, Math.max(min, value));
  }

  function roundToStep(value, step, min, max) {
    const rounded = Math.round(value / step) * step;
    const decimals = (() => {
      const s = String(step);
      const i = s.indexOf('.');
      return i < 0 ? 0 : s.length - i - 1;
    })();
    const fixed = decimals > 0 ? Number(rounded.toFixed(decimals)) : Math.round(rounded);
    return clamp(fixed, min, max);
  }

  function roundPct(percent) {
    return Math.round(percent * 100) / 100;
  }

  function applyToxicityToRr(rrPoints, adsPerUserPerDay, settings) {
    const maxSafe = settings.maxSafeAdsPerUser ?? CONFIG.DEFAULT_SETTINGS.maxSafeAdsPerUser;
    const penaltyPer = settings.retentionPenaltyPerExtraAd ?? CONFIG.DEFAULT_SETTINGS.retentionPenaltyPerExtraAd;
    const extra = Math.max(0, adsPerUserPerDay - maxSafe);
    const penaltyFraction = extra * penaltyPer;
    const factor = Math.max(0, 1 - penaltyFraction);
    const scaled = {
      rrDay1: roundPct(rrPoints.rrDay1 * factor),
      rrDay7: roundPct(rrPoints.rrDay7 * factor),
      rrDay14: roundPct(rrPoints.rrDay14 * factor),
    };
    let warning = null;
    if (extra > 0) {
      warning =
        `Внимание: вы превысили безопасный лимит рекламы (${maxSafe} показов). ` +
        `Кривая Rolling Retention уменьшена на ${(penaltyFraction * 100).toFixed(1)}% ` +
        `(RR D1 ≈ ${scaled.rrDay1.toFixed(2)}%).`;
    }
    return { ...scaled, extraAds: extra, penaltyFraction, warning, factor };
  }

  /** Log-linear interpolate between (d0,v0) and (d1,v1). */
  function interpLog(d, d0, v0, d1, v1) {
    if (d <= d0) return v0;
    if (d >= d1) return v1;
    if (v0 <= 0 || v1 <= 0) {
      const t = (d - d0) / (d1 - d0);
      return v0 + (v1 - v0) * t;
    }
    const t = (d - d0) / (d1 - d0);
    return Math.exp(Math.log(v0) + (Math.log(v1) - Math.log(v0)) * t);
  }

  /**
   * Build AppMetrica-style RR(d) in percent, RR(0)=100, non-increasing.
   * Anchors: D1, D7, D14; after D14 — decay with D7→D14 slope.
   */
  function buildRollingRetentionCurve(rrDay1, rrDay7, rrDay14, days) {
    let p1 = clamp(rrDay1, 0, 100);
    let p7 = clamp(rrDay7, 0, p1);
    let p14 = clamp(rrDay14, 0, p7);
    const RR = new Array(days + 1);
    RR[0] = 100;
    for (let d = 1; d <= days; d++) {
      let v;
      if (d <= 1) v = p1;
      else if (d <= 7) v = interpLog(d, 1, p1, 7, p7);
      else if (d <= 14) v = interpLog(d, 7, p7, 14, p14);
      else {
        // Continue D7→D14 decay in log space
        if (p7 > 0 && p14 > 0 && p14 < p7) {
          const dailyLog = (Math.log(p14) - Math.log(p7)) / 7;
          v = p14 * Math.exp(dailyLog * (d - 14));
        } else if (p14 > 0) {
          v = p14 * Math.pow(0.98, d - 14);
        } else {
          v = 0;
        }
      }
      v = clamp(v, 0, 100);
      if (d > 0) v = Math.min(v, RR[d - 1]);
      RR[d] = v;
    }
    return { RR, anchors: { rrDay1: p1, rrDay7: p7, rrDay14: p14 } };
  }

  /**
   * Convert Rolling Retention % curve → daily activity fraction for DAU.
   * activityFactor = classicDay1 / rrDay1 (AppMetrica right/left chart day 1).
   */
  function buildActiveRetention(RR, classicDay1, rrDay1) {
    const factor =
      rrDay1 > 0 ? clamp(classicDay1 / rrDay1, 0.01, 1) : 0.4;
    const R = new Array(RR.length);
    for (let d = 0; d < RR.length; d++) {
      if (d === 0) {
        R[d] = 1; // install day
      } else {
        R[d] = clamp((RR[d] / 100) * factor, 0, 1);
      }
    }
    // Keep non-increasing after day 0 for stability
    for (let d = 2; d < R.length; d++) {
      if (R[d] > R[d - 1]) R[d] = R[d - 1];
    }
    return { R, activityFactor: factor };
  }

  function buildPrefixFrom1(R) {
    const prefixFrom1 = new Array(R.length);
    prefixFrom1[0] = 0;
    let sum = 0;
    for (let d = 1; d < R.length; d++) {
      sum += R[d];
      prefixFrom1[d] = sum;
    }
    return prefixFrom1;
  }

  function lifetimeSum(R) {
    let sum = 0;
    for (let d = 0; d < R.length; d++) sum += R[d];
    return sum;
  }

  function revenuePerUserPerDay(adsPerUserPerDay, cpm) {
    return adsPerUserPerDay * (cpm / 1000);
  }

  function requiredDau(monthlyTarget, adsPerUserPerDay, cpm) {
    const perUser = revenuePerUserPerDay(adsPerUserPerDay, cpm);
    if (perUser <= 0) return Infinity;
    return (monthlyTarget / 30) / perUser;
  }

  function describeDynamics(dau, plateauDau, days) {
    let dauMin = dau[0];
    let dauMax = dau[0];
    for (let t = 1; t <= days; t++) {
      if (dau[t] < dauMin) dauMin = dau[t];
      if (dau[t] > dauMax) dauMax = dau[t];
    }
    const span = Math.max(Math.abs(plateauDau), Math.abs(dauMax - dauMin), 1);
    const tol = Math.max(0.5, span * 0.02);
    let nearPlateauDay = null;
    for (let t = 0; t <= days; t++) {
      if (Math.abs(dau[t] - plateauDau) <= tol) {
        let stable = true;
        const checkTo = Math.min(days, t + 4);
        for (let k = t; k <= checkTo; k++) {
          if (Math.abs(dau[k] - plateauDau) > tol) {
            stable = false;
            break;
          }
        }
        if (stable) {
          nearPlateauDay = t;
          break;
        }
      }
    }
    let endDay = nearPlateauDay != null ? nearPlateauDay + 14 : Math.min(90, days);
    endDay = Math.max(21, Math.min(days, endDay));
    if (nearPlateauDay == null) endDay = Math.min(days, 120);
    return { endDay, nearPlateauDay, dauMin, dauMax };
  }

  function audienceYDomain(sim) {
    const dauMin = sim.dauMin != null ? sim.dauMin : Math.min(...sim.dau);
    const dauMax = Math.max(
      sim.dauMax != null ? sim.dauMax : Math.max(...sim.dau),
      sim.plateauDau || 0,
    );
    const pad = Math.max(5, (dauMax - dauMin) * 0.18, dauMax * 0.05);
    let yMin = Math.max(0, dauMin - pad);
    let yMax = dauMax + pad;
    if (yMax <= yMin) yMax = yMin + 10;
    const req = sim.requiredDau;
    const reqFinite = Number.isFinite(req) && req > 0;
    const includeRequiredInScale = reqFinite && req <= yMax * 1.35 && req >= yMin * 0.5;
    if (includeRequiredInScale) {
      yMax = Math.max(yMax, req * 1.08);
      yMin = Math.min(yMin, Math.max(0, req * 0.92));
    }
    return { yMin, yMax, includeRequiredInScale, requiredDau: req };
  }

  function financeYDomain(data, mode, markers) {
    let dMin = data[0];
    let dMax = data[0];
    for (let i = 1; i < data.length; i++) {
      if (data[i] < dMin) dMin = data[i];
      if (data[i] > dMax) dMax = data[i];
    }
    const pad = Math.max(1, (dMax - dMin) * 0.12, dMax * 0.05);
    const yMin = 0;
    let yMax = Math.max(dMax + pad, 10);
    const allMarkers = markers || [];
    const visibleMarkers = allMarkers.filter((m) => m <= yMax * 1.2);
    const nextMarker = allMarkers.find((m) => m > dMax);
    if (nextMarker != null && nextMarker <= dMax * 1.5) {
      yMax = Math.max(yMax, nextMarker * 1.05);
      if (!visibleMarkers.includes(nextMarker)) visibleMarkers.push(nextMarker);
    }
    return { yMin, yMax, visibleMarkers, mode };
  }

  function simulate(inputs, settings) {
    const days = CONFIG.DAYS;
    const ads = inputs.adsPerUserPerDay;
    const currentDau = Math.max(0, Math.round(Number(inputs.currentDau) || 0));
    const installsPerDay = Math.max(0, Math.round(Number(inputs.installsPerDay) || 0));
    const classicDay1 = clamp(Number(inputs.classicDay1) || CONFIG.DEFAULT_INPUTS.classicDay1, 0.01, 100);

    const tox = applyToxicityToRr(
      {
        rrDay1: inputs.rrDay1,
        rrDay7: inputs.rrDay7,
        rrDay14: inputs.rrDay14,
      },
      ads,
      settings || CONFIG.DEFAULT_SETTINGS,
    );

    const { RR, anchors } = buildRollingRetentionCurve(tox.rrDay1, tox.rrDay7, tox.rrDay14, days);
    const { R, activityFactor } = buildActiveRetention(RR, classicDay1, anchors.rrDay1);
    const prefixFrom1 = buildPrefixFrom1(R);

    const dau = new Array(days + 1);
    const dailyRevenue = new Array(days + 1);
    const cumulative = new Array(days + 1);
    const rolling30 = new Array(days + 1);

    const rpu = revenuePerUserPerDay(ads, inputs.cpm);
    const reqDau = requiredDau(inputs.monthlyTarget, ads, inputs.cpm);

    let cum = 0;
    let rollWindow = 0;
    for (let t = 0; t <= days; t++) {
      const legacy = currentDau * R[t];
      const cohort = t === 0 ? 0 : installsPerDay * (1 + prefixFrom1[t - 1]);
      const d = legacy + cohort;
      dau[t] = d;
      const rev = d * rpu;
      dailyRevenue[t] = rev;
      cum += rev;
      cumulative[t] = cum;
      rollWindow += rev;
      if (t >= 30) rollWindow -= dailyRevenue[t - 30];
      rolling30[t] = rollWindow;
    }

    const L = lifetimeSum(R);
    const plateauDau = installsPerDay * L;

    let goalDay = null;
    for (let t = 0; t <= days; t++) {
      if (dau[t] >= reqDau) {
        goalDay = t;
        break;
      }
    }

    const dynamics = describeDynamics(dau, plateauDau, days);

    return {
      days,
      dau,
      dailyRevenue,
      cumulative,
      rolling30,
      requiredDau: reqDau,
      plateauDau,
      goalDay,
      dynamicsEndDay: dynamics.endDay,
      nearPlateauDay: dynamics.nearPlateauDay,
      dauMin: dynamics.dauMin,
      dauMax: dynamics.dauMax,
      reachableWithinHorizon: goalDay !== null,
      unreachable: goalDay === null && plateauDau < reqDau - 1e-9,
      toxicity: tox,
      rrCurve: RR,
      activeR: R,
      activityFactor,
      rrAnchors: anchors,
      classicDay1,
      lifetimeDays: L,
      currentDailyRevenue: dailyRevenue[0],
      revenuePerUserPerDay: rpu,
    };
  }

  /** Scale all RR anchors by s (>=0), keeping shape; used by solver. */
  function scaleRrInputs(inputs, scale) {
    const s = Math.max(0, scale);
    return {
      ...inputs,
      rrDay1: roundPct(clamp(inputs.rrDay1 * s, CONFIG.RR_MIN, CONFIG.RR_MAX)),
      rrDay7: roundPct(clamp(inputs.rrDay7 * s, CONFIG.RR_MIN, CONFIG.RR_MAX)),
      rrDay14: roundPct(clamp(inputs.rrDay14 * s, CONFIG.RR_MIN, CONFIG.RR_MAX)),
    };
  }

  function normalizeInputs(raw) {
    let rr1 = roundToStep(
      Number(raw.rrDay1) || CONFIG.DEFAULT_INPUTS.rrDay1,
      CONFIG.RR_STEP,
      CONFIG.RR_MIN,
      CONFIG.RR_MAX,
    );
    let rr7 = roundToStep(
      Number(raw.rrDay7) || CONFIG.DEFAULT_INPUTS.rrDay7,
      CONFIG.RR_STEP,
      CONFIG.RR_MIN,
      CONFIG.RR_MAX,
    );
    let rr14 = roundToStep(
      Number(raw.rrDay14) || CONFIG.DEFAULT_INPUTS.rrDay14,
      CONFIG.RR_STEP,
      CONFIG.RR_MIN,
      CONFIG.RR_MAX,
    );
    // Enforce non-increasing AppMetrica-style curve
    rr7 = Math.min(rr7, rr1);
    rr14 = Math.min(rr14, rr7);
    let classic1 = roundToStep(
      Number(raw.classicDay1) || CONFIG.DEFAULT_INPUTS.classicDay1,
      CONFIG.CLASSIC_STEP,
      CONFIG.CLASSIC_MIN,
      CONFIG.CLASSIC_MAX,
    );
    // Classic D1 cannot exceed RR D1 in AppMetrica
    classic1 = Math.min(classic1, rr1);

    return {
      monthlyTarget: roundToStep(
        Number(raw.monthlyTarget) || CONFIG.DEFAULT_INPUTS.monthlyTarget,
        CONFIG.MONTHLY_TARGET_STEP,
        CONFIG.MONTHLY_TARGET_MIN,
        CONFIG.MONTHLY_TARGET_MAX,
      ),
      cpm: roundToStep(
        Number(raw.cpm) || CONFIG.DEFAULT_INPUTS.cpm,
        CONFIG.CPM_STEP,
        CONFIG.CPM_MIN,
        CONFIG.CPM_MAX,
      ),
      adsPerUserPerDay: roundToStep(
        Number(raw.adsPerUserPerDay) || CONFIG.DEFAULT_INPUTS.adsPerUserPerDay,
        CONFIG.ADS_PER_USER_STEP,
        CONFIG.ADS_PER_USER_MIN,
        CONFIG.ADS_PER_USER_MAX,
      ),
      currentInstalls: Math.max(0, Math.round(Number(raw.currentInstalls) || 0)),
      currentDau: Math.max(0, Math.round(Number(raw.currentDau) || 0)),
      installsPerDay: roundToStep(
        Number(raw.installsPerDay) ?? CONFIG.DEFAULT_INPUTS.installsPerDay,
        CONFIG.INSTALLS_PER_DAY_STEP,
        CONFIG.INSTALLS_PER_DAY_MIN,
        CONFIG.INSTALLS_PER_DAY_MAX,
      ),
      rrDay1: rr1,
      rrDay7: rr7,
      rrDay14: rr14,
      classicDay1: classic1,
    };
  }

  const api = {
    clamp,
    roundToStep,
    roundPct,
    applyToxicityToRr,
    buildRollingRetentionCurve,
    buildActiveRetention,
    lifetimeSum,
    revenuePerUserPerDay,
    requiredDau,
    simulate,
    scaleRrInputs,
    describeDynamics,
    audienceYDomain,
    financeYDomain,
    normalizeInputs,
  };

  root.AliceMonetization = root.AliceMonetization || {};
  Object.assign(root.AliceMonetization, api);

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = { CONFIG, ...api };
  }
})(typeof globalThis !== 'undefined' ? globalThis : window);

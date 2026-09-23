/**
 * Node tests — AppMetrica Rolling Retention model.
 * Run: node --test admin-web/js/monetization-calculator/model.test.mjs
 */
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const require = createRequire(import.meta.url);

require(join(__dirname, 'config.js'));
require(join(__dirname, 'model.js'));
require(join(__dirname, 'solver.js'));

const M = globalThis.AliceMonetization;
const SETTINGS = { ...M.CONFIG.DEFAULT_SETTINGS };

const APPMETRICA_SAMPLE = {
  monthlyTarget: 10000,
  cpm: 260,
  adsPerUserPerDay: 3.5,
  currentInstalls: 1800,
  currentDau: 60,
  installsPerDay: 70,
  rrDay1: 17.75,
  rrDay7: 8.62,
  rrDay14: 6.33,
  classicDay1: 7.19,
};

describe('AppMetrica RR curve', () => {
  it('RR(0)=100 and anchors match inputs (non-increasing)', () => {
    const { RR } = M.buildRollingRetentionCurve(17.75, 8.62, 6.33, 365);
    assert.equal(RR[0], 100);
    assert.ok(Math.abs(RR[1] - 17.75) < 0.02);
    assert.ok(Math.abs(RR[7] - 8.62) < 0.05);
    assert.ok(Math.abs(RR[14] - 6.33) < 0.05);
    for (let d = 1; d <= 60; d++) assert.ok(RR[d] <= RR[d - 1] + 1e-9);
  });

  it('activityFactor = classicD1 / rrD1', () => {
    const { RR } = M.buildRollingRetentionCurve(17.75, 8.62, 6.33, 30);
    const { R, activityFactor } = M.buildActiveRetention(RR, 7.19, 17.75);
    assert.ok(Math.abs(activityFactor - 7.19 / 17.75) < 1e-9);
    assert.equal(R[0], 1);
    assert.ok(Math.abs(R[1] - (17.75 / 100) * activityFactor) < 1e-9);
  });
});

describe('requiredDau', () => {
  it('matches monthly target formula', () => {
    assert.equal(M.requiredDau(30000, 4, 250), 1000);
  });
});

describe('simulate', () => {
  it('day 0 DAU = currentDau; revenue ≈ 55 for sample', () => {
    const sim = M.simulate(M.normalizeInputs(APPMETRICA_SAMPLE), SETTINGS);
    assert.equal(sim.dau[0], 60);
    assert.ok(Math.abs(sim.currentDailyRevenue - 54.6) < 0.05);
    assert.ok(sim.activityFactor > 0.3 && sim.activityFactor < 0.5);
  });

  it('plateau = installs * lifetime active days', () => {
    const inputs = M.normalizeInputs({
      ...APPMETRICA_SAMPLE,
      currentDau: 0,
      installsPerDay: 100,
    });
    const sim = M.simulate(inputs, SETTINGS);
    assert.ok(Math.abs(sim.plateauDau - 100 * sim.lifetimeDays) < 1e-6);
    assert.ok(sim.lifetimeDays > 1);
  });

  it('stronger RR raises plateau', () => {
    const weak = M.simulate(
      M.normalizeInputs({ ...APPMETRICA_SAMPLE, currentDau: 0, rrDay1: 10, rrDay7: 5, rrDay14: 3, classicDay1: 4 }),
      SETTINGS,
    );
    const strong = M.simulate(
      M.normalizeInputs({ ...APPMETRICA_SAMPLE, currentDau: 0, rrDay1: 40, rrDay7: 20, rrDay14: 12, classicDay1: 16 }),
      SETTINGS,
    );
    assert.ok(strong.plateauDau > weak.plateauDau);
  });

  it('cumulative monotonic; daily revenue tracks DAU', () => {
    const sim = M.simulate(M.normalizeInputs(APPMETRICA_SAMPLE), SETTINGS);
    for (let t = 1; t <= sim.days; t++) {
      assert.ok(sim.cumulative[t] >= sim.cumulative[t - 1] - 1e-9);
    }
    const rpu = sim.revenuePerUserPerDay;
    for (const t of [0, 1, 7, 30]) {
      assert.ok(Math.abs(sim.dailyRevenue[t] - sim.dau[t] * rpu) < 1e-6);
    }
  });

  it('audienceYDomain does not stretch to far required', () => {
    const sim = M.simulate(M.normalizeInputs(APPMETRICA_SAMPLE), SETTINGS);
    const y = M.audienceYDomain(sim);
    if (sim.requiredDau > sim.plateauDau * 1.5) {
      assert.equal(y.includeRequiredInScale, false);
      assert.ok(y.yMax < sim.requiredDau * 0.7);
    }
  });
});

describe('solver', () => {
  it('min traffic finds higher installs when unreachable', () => {
    const inputs = M.normalizeInputs({
      ...APPMETRICA_SAMPLE,
      monthlyTarget: 200000,
      installsPerDay: 5,
    });
    const sol = M.solveMinTraffic(inputs, SETTINGS);
    if (sol.feasible && !sol.alreadyOk) {
      assert.ok(sol.installsPerDay > inputs.installsPerDay);
      assert.ok(M.hitsGoal({ ...inputs, installsPerDay: sol.installsPerDay }, SETTINGS));
    }
  });

  it('normalizeInputs enforces RR D1≥D7≥D14 and classic≤RR D1', () => {
    const n = M.normalizeInputs({
      rrDay1: 10,
      rrDay7: 20,
      rrDay14: 15,
      classicDay1: 50,
    });
    assert.ok(n.rrDay7 <= n.rrDay1);
    assert.ok(n.rrDay14 <= n.rrDay7);
    assert.ok(n.classicDay1 <= n.rrDay1);
  });

  it('normalizeInputs snaps ads to 0.1', () => {
    assert.equal(M.normalizeInputs({ adsPerUserPerDay: 3.54 }).adsPerUserPerDay, 3.5);
  });
});

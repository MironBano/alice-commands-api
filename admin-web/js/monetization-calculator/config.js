/**
 * Defaults for monetization calculator — AppMetrica Rolling Retention.
 */
(function (root) {
  const CONFIG = Object.freeze({
    DAYS: 365,
    MONTHLY_TARGET_MIN: 1000,
    MONTHLY_TARGET_MAX: 500000,
    MONTHLY_TARGET_STEP: 200,
    CPM_MIN: 50,
    CPM_MAX: 2000,
    CPM_STEP: 10,
    ADS_PER_USER_MIN: 1,
    ADS_PER_USER_MAX: 20,
    ADS_PER_USER_STEP: 0.1,
    INSTALLS_PER_DAY_MIN: 0,
    INSTALLS_PER_DAY_MAX: 10000,
    INSTALLS_PER_DAY_STEP: 1,
    /** AppMetrica Rolling Retention % (returned on day N or any later day). */
    RR_MIN: 0.01,
    RR_MAX: 100,
    RR_STEP: 0.01,
    CLASSIC_MIN: 0.01,
    CLASSIC_MAX: 100,
    CLASSIC_STEP: 0.01,
    CUMULATIVE_MARKERS: [50000, 100000, 500000],
    STORAGE_KEY: 'alice_monetization_calculator_settings',
    /**
     * Defaults close to AliceCommands AppMetrica screenshot (Rolling vs Classic).
     * RR = left chart; classicDay1 = right chart day 1 (for DAU conversion).
     */
    DEFAULT_INPUTS: Object.freeze({
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
    }),
    DEFAULT_SETTINGS: Object.freeze({
      maxSafeAdsPerUser: 4,
      retentionPenaltyPerExtraAd: 0.02,
      balanceWeightInstalls: 1,
      balanceWeightRetention: 1,
    }),
  });

  root.AliceMonetization = root.AliceMonetization || {};
  root.AliceMonetization.CONFIG = CONFIG;
})(typeof globalThis !== 'undefined' ? globalThis : window);

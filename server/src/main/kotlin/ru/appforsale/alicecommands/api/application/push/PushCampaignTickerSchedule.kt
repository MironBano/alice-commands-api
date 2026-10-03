package ru.appforsale.alicecommands.api.application.push

/**
 * Aligns campaign ticks to clock quarters (:00, :15, :30, :45) so S2 COD ±15m windows are hit.
 */
object PushCampaignTickerSchedule {
    const val QUARTER_MS: Long = 15L * 60L * 1000L

    /**
     * Delay until the next quarter-hour boundary.
     * If [nowMs] is already aligned (within [toleranceMs] after a boundary), returns 0.
     */
    fun delayMsUntilAlignedTick(nowMs: Long, toleranceMs: Long = 1_000L): Long {
        require(nowMs >= 0L) { "nowMs must be non-negative" }
        val rem = nowMs % QUARTER_MS
        if (rem <= toleranceMs) return 0L
        return QUARTER_MS - rem
    }

    /** Always waits for the following quarter (never 0), used after a tick runs. */
    fun delayMsUntilNextQuarterStrict(nowMs: Long): Long {
        require(nowMs >= 0L) { "nowMs must be non-negative" }
        val rem = nowMs % QUARTER_MS
        return if (rem == 0L) QUARTER_MS else QUARTER_MS - rem
    }
}

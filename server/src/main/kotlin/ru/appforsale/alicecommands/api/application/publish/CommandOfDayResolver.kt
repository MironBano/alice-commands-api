package ru.appforsale.alicecommands.api.application.publish

import ru.appforsale.alicecommands.api.domain.Command
import java.time.LocalDate
import java.time.ZoneId

object CommandOfDayResolver {

    val EDITORIAL_ZONE: ZoneId = ZoneId.of("Europe/Moscow")
    const val DEFAULT_SEED: Int = 31
    private const val NULL_SORT_ORDER = Int.MAX_VALUE

    fun buildPool(commands: List<Command>, categoryId: String): List<Command> =
        commands
            .filter { it.category_id == categoryId }
            .sortedWith(
                compareBy<Command> { it.sort_order ?: NULL_SORT_ORDER }
                    .thenBy { it.id },
            )

    fun resolveCommandId(
        pool: List<Command>,
        date: LocalDate,
        seed: Int = DEFAULT_SEED,
    ): String {
        require(pool.isNotEmpty()) { "Command of day pool is empty for category" }
        val epochDay = date.toEpochDay()
        val poolSize = pool.size
        val effectiveSeed = if (seed >= 1) seed else DEFAULT_SEED
        val index = ((epochDay * effectiveSeed) + poolSize).mod(poolSize).toInt()
        return pool[index].id
    }

    fun todayMoscow(): LocalDate = LocalDate.now(EDITORIAL_ZONE)
}

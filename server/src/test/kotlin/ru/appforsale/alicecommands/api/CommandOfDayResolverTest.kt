package ru.appforsale.alicecommands.api.application.publish

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.domain.Command
import java.time.LocalDate

class CommandOfDayResolverTest {

    private fun command(id: String, categoryId: String = "test_cat", sortOrder: Int? = null) = Command(
        id = id,
        category_id = categoryId,
        title_ru = "Title $id",
        phrases = listOf("Алиса, $id"),
        effect_description_ru = "Effect",
        requires_alice_word = true,
        source_url = "https://example.com",
        updated_at = "2026-07-01T00:00:00Z",
        sort_order = sortOrder,
    )

    @Test
    fun `shared vector 2026-07-01 picks deterministic command`() {
        val pool = listOf(command("a", sortOrder = 1), command("b", sortOrder = 2), command("c", sortOrder = 3))
        val sorted = CommandOfDayResolver.buildPool(pool, "test_cat")
        val date = LocalDate.parse("2026-07-01")
        val result = CommandOfDayResolver.resolveCommandId(sorted, date, seed = 31)
        val epochDay = date.toEpochDay()
        val index = ((epochDay * 31) + sorted.size).mod(sorted.size).toInt()
        assertEquals(sorted[index].id, result)
        assertEquals("b", result)
    }

    @Test
    fun `shared vector 2026-07-02 differs from previous day`() {
        val pool = listOf(command("a", sortOrder = 1), command("b", sortOrder = 2), command("c", sortOrder = 3))
        val sorted = CommandOfDayResolver.buildPool(pool, "test_cat")
        val day1 = CommandOfDayResolver.resolveCommandId(sorted, LocalDate.parse("2026-07-01"), seed = 31)
        val day2 = CommandOfDayResolver.resolveCommandId(sorted, LocalDate.parse("2026-07-02"), seed = 31)
        assertEquals("b", day1)
        assertEquals("c", day2)
    }

    @Test
    fun `tie-break by id when sort_order equal`() {
        val commands = listOf(
            command("b", sortOrder = 1),
            command("a", sortOrder = 1),
            command("c", sortOrder = 2),
        )
        val pool = CommandOfDayResolver.buildPool(commands, "test_cat")
        assertEquals(listOf("a", "b", "c"), pool.map { it.id })
        val date = LocalDate.parse("2026-07-01")
        val index = ((date.toEpochDay() * 31) + pool.size).mod(pool.size).toInt()
        assertEquals(pool[index].id, CommandOfDayResolver.resolveCommandId(pool, date, seed = 31))
    }

    @Test
    fun `empty pool throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            CommandOfDayResolver.resolveCommandId(emptyList(), LocalDate.parse("2026-07-01"))
        }
    }

    @Test
    fun `null sort_order sorts last`() {
        val commands = listOf(
            command("z", sortOrder = null),
            command("a", sortOrder = 1),
        )
        val pool = CommandOfDayResolver.buildPool(commands, "test_cat")
        assertEquals(listOf("a", "z"), pool.map { it.id })
    }
}

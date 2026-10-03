package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.application.BundleCodec
import ru.appforsale.alicecommands.api.application.push.PublishedBundleReader
import ru.appforsale.alicecommands.api.domain.CommandOfDay
import ru.appforsale.alicecommands.api.domain.ContentBundle

class PublishedBundleReaderTest {

    @Test
    fun extractCommandOfDayId_fromGzipBundle() {
        val bundle = ContentBundle(
            schema_version = 2,
            content_version = 15,
            published_at = "2026-09-29T07:05:45Z",
            command_of_day = CommandOfDay(
                mode = "auto",
                command_id = "station_settings_vkliuchi_ekvalaizer",
                resolved_date = "2026-09-29",
                updated_at = "2026-08-08T14:03:50Z",
            ),
        )
        val gzip = BundleCodec.gzip(BundleCodec.toJson(bundle))
        assertEquals(
            "station_settings_vkliuchi_ekvalaizer",
            PublishedBundleReader.extractCommandOfDayId(gzip),
        )
    }

    @Test
    fun extractCommandOfDayId_fromRawJson() {
        val json = """
            {"schema_version":2,"content_version":1,"published_at":"2026-01-01T00:00:00Z",
             "command_of_day":{"mode":"manual","command_id":"music_muzyka",
             "resolved_date":"2026-01-01","updated_at":"2026-01-01T00:00:00Z"}}
        """.trimIndent()
        assertEquals("music_muzyka", PublishedBundleReader.extractCommandOfDayId(json.toByteArray()))
    }

    @Test
    fun extractCommandOfDayId_garbageReturnsNull() {
        assertNull(PublishedBundleReader.extractCommandOfDayId(byteArrayOf(1, 2, 3, 4)))
        assertNull(PublishedBundleReader.extractCommandOfDayId(ByteArray(0)))
    }
}

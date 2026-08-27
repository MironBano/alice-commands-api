package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.application.publish.resolveDetailReferralPickIds
import ru.appforsale.alicecommands.api.domain.DeviceGuide
import ru.appforsale.alicecommands.api.domain.DevicePick

class DeviceGuideReferralPicksTest {

    @Test
    fun `washing machine detail puts primary pick first then related`() {
        val guide = DeviceGuide(
            id = "washing_machine",
            title_ru = "Стиральная машина",
            summary_ru = "s",
            capabilities_ru = "c",
            setup_ru = "u",
            related_device_ids = listOf("sensor_water_leak", "sensor_vibration", "valve"),
            action_url = "https://example.com/guide",
            sort_order = 310,
        )
        val picksById = mapOf(
            "pick_washing_machine" to detailPick("pick_washing_machine"),
            "pick_sensor_water_leak" to detailPick("pick_sensor_water_leak"),
            "pick_sensor_vibration" to detailPick("pick_sensor_vibration"),
            "pick_valve" to detailPick("pick_valve"),
        )

        assertEquals(
            listOf(
                "pick_washing_machine",
                "pick_sensor_water_leak",
                "pick_sensor_vibration",
                "pick_valve",
            ),
            resolveDetailReferralPickIds(guide, picksById),
        )
    }

    @Test
    fun `skips related picks without device_guide_detail placement`() {
        val guide = DeviceGuide(
            id = "station",
            title_ru = "Колонка",
            summary_ru = "s",
            capabilities_ru = "c",
            setup_ru = "u",
            related_device_ids = listOf("phone", "hub", "light"),
            action_url = "https://example.com/station",
            sort_order = 10,
        )
        val picksById = mapOf(
            "pick_station" to pick("pick_station", placements = listOf("smart_home_devices")),
            "pick_phone" to pick("pick_phone", placements = listOf("smart_home_devices")),
            "pick_hub" to detailPick("pick_hub"),
            "pick_light" to pick("pick_light", placements = listOf("smart_home_devices", "command_detail")),
        )

        assertEquals(
            listOf("pick_station", "pick_hub"),
            resolveDetailReferralPickIds(guide, picksById),
        )
    }

    private fun detailPick(id: String) = pick(id, placements = listOf("device_guide_detail"))

    private fun pick(id: String, placements: List<String>) = DevicePick(
        id = id,
        title_ru = id,
        action_url = "https://market.yandex.ru/search?text=test",
        sort_order = 1,
        placements = placements,
    )
}

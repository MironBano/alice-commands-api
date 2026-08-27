package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.appforsale.alicecommands.api.application.publish.PublishSmartHomeDevicesUseCase
import ru.appforsale.alicecommands.api.application.publish.SmartHomeDevicesValidationUseCase
import ru.appforsale.alicecommands.api.domain.DeviceGuide
import ru.appforsale.alicecommands.api.domain.DevicePick
import ru.appforsale.alicecommands.api.domain.SmartHomeDevicesResponse
import ru.appforsale.alicecommands.api.infrastructure.validation.JsonSmartHomeDevicesSchemaValidator
import ru.appforsale.alicecommands.api.application.BundleCodec
import java.nio.file.Path
import kotlin.io.path.Path

class SmartHomeDevicesPublishUseCaseTest {

    @Test
    fun `publish smart home devices writes public snapshot`() {
        val draft = object : AffiliatePublishUseCaseTest.FakeDraftRepository() {
            override fun listDeviceGuides() = listOf(STATION_GUIDE)
            override fun listDevicePicks() = listOf(SAMPLE_PICK)
        }
        val storage = AffiliatePublishUseCaseTest.FakeBundleStorage()
        val validation = SmartHomeDevicesValidationUseCase(setOf("example.com", "localhost", "127.0.0.1"))
        val schemaValidator = JsonSmartHomeDevicesSchemaValidator(resolveSchemaPath(), BundleCodec.json)

        val result = PublishSmartHomeDevicesUseCase(draft, storage, validation, schemaValidator)
            .execute(updatedAt = "2026-07-06T12:00:00Z")

        assertEquals(1, result.guides.size)
        assertEquals("station", result.guides.single().id)
        assertEquals(listOf("pick_station"), result.guides.single().detail_referral_pick_ids)
        assertEquals(1, result.picks.size)
        val public = storage.readSmartHomeDevices()
        assertNotNull(public)
        assertEquals("https://market.yandex.ru/product/1", public!!.picks.single().action_url)
    }

    @Test
    fun `validation rejects bad action url`() {
        val validation = SmartHomeDevicesValidationUseCase(setOf("example.com"))
        val errors = validation.validatePick(
            SAMPLE_PICK.copy(action_url = "http://insecure.example/product"),
        )
        assertTrue(errors.isNotEmpty())
    }

    @Test
    fun `validation accepts market url`() {
        val validation = SmartHomeDevicesValidationUseCase(setOf("example.com"))
        val errors = validation.validateActionUrl("action_url", "market://details?id=123")
        assertTrue(errors.isEmpty())
    }

    companion object {
        private val STATION_GUIDE = DeviceGuide(
            id = "station",
            title_ru = "Колонка",
            summary_ru = "Кратко",
            capabilities_ru = "Полный текст возможностей",
            setup_ru = "Полный текст подключения",
            setup_steps_ru = listOf("Шаг 1"),
            command_device_filter_id = "station",
            action_url = "https://alice.yandex.ru/support/ru/station/",
            sort_order = 10,
        )

        private val SAMPLE_PICK = DevicePick(
            id = "pick_station",
            title_ru = "Станция",
            description_ru = "Описание",
            price_hint_ru = "от 9 990 ₽",
            action_url = "https://market.yandex.ru/product/1",
            sort_order = 1,
        )

        private fun resolveSchemaPath(): Path {
            val candidates = listOf(
                Path("schema/smarthome-devices.schema.json"),
                Path("../schema/smarthome-devices.schema.json"),
            )
            return candidates.first { it.toFile().exists() }
        }
    }
}

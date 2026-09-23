package ru.appforsale.alicecommands.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.appforsale.alicecommands.api.application.affiliate.RefreshAffiliatePicksUseCase
import ru.appforsale.alicecommands.api.application.publish.PublishSmartHomeDevicesUseCase
import ru.appforsale.alicecommands.api.application.publish.SmartHomeDevicesValidationUseCase
import ru.appforsale.alicecommands.api.infrastructure.validation.JsonSmartHomeDevicesSchemaValidator
import ru.appforsale.alicecommands.api.application.BundleCodec
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.writeText

class RefreshAffiliatePicksUseCaseTest {

    @TempDir
    lateinit var tmp: Path

    @Test
    fun `disabled refresh is skipped`() {
        val useCase = buildUseCase(enabled = false, oauth = "", clid = "")
        val result = useCase.execute()
        assertTrue(result.skipped)
        assertEquals("MARKET_AFFILIATE_REFRESH_ENABLED=false", result.reason)
    }

    @Test
    fun `enabled without oauth is dry-run`() {
        val mapPath = tmp.resolve("map.json")
        mapPath.writeText(
            """
            {"picks":[{"pick_id":"pick_light","market_url_primary":"https://market.yandex.ru/product/1","erid":"x"}]}
            """.trimIndent(),
        )
        val useCase = buildUseCase(
            enabled = true,
            oauth = "",
            clid = "",
            mapPath = mapPath,
        )
        val result = useCase.execute()
        assertTrue(result.skipped)
        assertEquals("oauth_or_clid_missing", result.reason)
    }

    private fun buildUseCase(
        enabled: Boolean,
        oauth: String,
        clid: String,
        mapPath: Path = tmp.resolve("empty.json"),
    ): RefreshAffiliatePicksUseCase {
        val draft = AffiliatePublishUseCaseTest.FakeDraftRepository()
        val storage = AffiliatePublishUseCaseTest.FakeBundleStorage()
        val validation = SmartHomeDevicesValidationUseCase(setOf("example.com", "localhost"))
        val schema = JsonSmartHomeDevicesSchemaValidator(
            listOf(
                Path("schema/smarthome-devices.schema.json"),
                Path("../schema/smarthome-devices.schema.json"),
            ).first { it.toFile().exists() },
            BundleCodec.json,
        )
        val publish = PublishSmartHomeDevicesUseCase(draft, storage, validation, schema)
        return RefreshAffiliatePicksUseCase(
            draftRepository = draft,
            publishSmartHomeDevicesUseCase = publish,
            pickValidator = validation,
            pickSkuMapPath = mapPath,
            oauthToken = oauth,
            clid = clid,
            enabled = enabled,
        )
    }
}

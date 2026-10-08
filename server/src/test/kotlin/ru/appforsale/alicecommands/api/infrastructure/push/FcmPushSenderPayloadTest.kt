package ru.appforsale.alicecommands.api.infrastructure.push

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FcmPushSenderPayloadTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun buildFcmSendPayload_includesNotificationDataAndChannel() {
        val payload = buildFcmSendPayloadJson(
            json = json,
            token = "tok-1",
            title = "Title",
            body = "Body",
            data = mapOf("scenario" to "s1"),
            channelId = "push_content",
            deeplink = "alicecommands://route/home/catalog?source=push",
        )
        assertContains(payload, "\"token\":\"tok-1\"")
        assertContains(payload, "\"title\":\"Title\"")
        assertContains(payload, "\"body\":\"Body\"")
        assertContains(payload, "\"scenario\":\"s1\"")
        assertContains(payload, "\"channel_id\":\"push_content\"")
        assertContains(payload, "alicecommands://route/home/catalog")
        assertTrue(payload.contains("\"message\""))
        // FCM click_action is an Intent action, not a deeplink URI.
        assertFalse(payload.contains("\"click_action\""))
    }
}

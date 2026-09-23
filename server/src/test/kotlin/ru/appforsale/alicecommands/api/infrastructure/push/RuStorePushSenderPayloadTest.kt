package ru.appforsale.alicecommands.api.infrastructure.push

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuStorePushSenderPayloadTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun payload_alwaysIncludesNotificationChannelAndClickAction() {
        val encoded = buildRuStoreSendPayloadJson(
            json = json,
            token = "tok-1",
            title = "Title",
            body = "Body",
            data = mapOf("scenario" to "s2"),
            channelId = "push_reminder",
            deeplink = "alicecommands://route/home/catalog?source=push",
        )
        val root = json.parseToJsonElement(encoded).jsonObject
        val message = root.getValue("message").jsonObject
        assertTrue(message.containsKey("notification"))
        assertEquals("Title", message.getValue("notification").jsonObject.getValue("title").jsonPrimitive.content)
        assertEquals("Body", message.getValue("notification").jsonObject.getValue("body").jsonPrimitive.content)

        val androidNotification = message.getValue("android").jsonObject
            .getValue("notification").jsonObject
        assertEquals("push_reminder", androidNotification.getValue("channel_id").jsonPrimitive.content)
        assertEquals(
            "alicecommands://route/home/catalog?source=push",
            androidNotification.getValue("click_action").jsonPrimitive.content,
        )
        assertEquals(1, androidNotification.getValue("click_action_type").jsonPrimitive.content.toInt())
    }

    @Test
    fun payload_fallsBackToPushContentWhenChannelBlankOrNull() {
        listOf(null, "", "   ").forEach { channel ->
            val encoded = buildRuStoreSendPayloadJson(
                json = json,
                token = "tok-1",
                title = "T",
                body = "B",
                data = emptyMap(),
                channelId = channel,
                deeplink = "alicecommands://route/home/catalog",
            )
            val channelId = json.parseToJsonElement(encoded).jsonObject
                .getValue("message").jsonObject
                .getValue("android").jsonObject
                .getValue("notification").jsonObject
                .getValue("channel_id").jsonPrimitive.content
            assertEquals(DEFAULT_RUSTORE_PUSH_CHANNEL_ID, channelId)
        }
    }
}

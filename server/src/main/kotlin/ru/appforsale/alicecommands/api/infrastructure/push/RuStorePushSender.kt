package ru.appforsale.alicecommands.api.infrastructure.push

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URI

interface RuStorePushSender {
    fun send(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>,
        channelId: String?,
        deeplink: String,
    ): Result<Unit>
}

class NoOpRuStorePushSender : RuStorePushSender {
    private val log = LoggerFactory.getLogger(NoOpRuStorePushSender::class.java)
    override fun send(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>,
        channelId: String?,
        deeplink: String,
    ): Result<Unit> {
        log.info(
            "dry-run push scenario={} tokenTail={} title={} deeplink={}",
            data["scenario"],
            token.takeLast(8),
            title,
            deeplink,
        )
        return Result.success(Unit)
    }
}

/**
 * RuStore Push HTTP API:
 * POST https://vkpns.rustore.ru/v1/projects/{projectId}/messages:send
 *
 * Deep link open requires android.notification.click_action + click_action_type=1
 * (RuStore app ≥ 1.39.0).
 */
class HttpRuStorePushSender(
    private val projectId: String,
    private val serviceToken: String,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) : RuStorePushSender {

    private val log = LoggerFactory.getLogger(HttpRuStorePushSender::class.java)

    override fun send(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>,
        channelId: String?,
        deeplink: String,
    ): Result<Unit> = runCatching {
        val url = URI(
            "https://vkpns.rustore.ru/v1/projects/$projectId/messages:send",
        ).toURL()
        val bodyBytes = buildRuStoreSendPayloadJson(
            json = json,
            token = token,
            title = title,
            body = body,
            data = data,
            channelId = channelId,
            deeplink = deeplink,
        ).toByteArray(Charsets.UTF_8)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $serviceToken")
            connectTimeout = 10_000
            readTimeout = 15_000
        }
        connection.outputStream.use { it.write(bodyBytes) }
        val code = connection.responseCode
        val responseText = runCatching {
            (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.readText().orEmpty()
        }.getOrDefault("")
        if (code !in 200..299) {
            log.warn("RuStore push failed status={} body={}", code, responseText.take(500))
            error("rustore_push_http_$code")
        }
    }
}

internal const val DEFAULT_RUSTORE_PUSH_CHANNEL_ID = "push_content"

/** Builds vkpns messages:send JSON. Blank/null channelId falls back to [DEFAULT_RUSTORE_PUSH_CHANNEL_ID]. */
internal fun buildRuStoreSendPayloadJson(
    json: Json,
    token: String,
    title: String,
    body: String,
    data: Map<String, String>,
    channelId: String?,
    deeplink: String,
): String {
    val resolvedChannel = channelId?.takeIf { it.isNotBlank() } ?: DEFAULT_RUSTORE_PUSH_CHANNEL_ID
    val payload = RuStoreSendRequest(
        message = RuStoreMessage(
            token = token,
            notification = RuStoreNotification(title = title, body = body),
            data = data,
            android = RuStoreAndroid(
                notification = RuStoreAndroidNotification(
                    title = title,
                    body = body,
                    channelId = resolvedChannel,
                    clickAction = deeplink,
                    clickActionType = 1,
                ),
            ),
        ),
    )
    return json.encodeToString(payload)
}

@Serializable
private data class RuStoreSendRequest(
    val message: RuStoreMessage,
)

@Serializable
private data class RuStoreMessage(
    val token: String,
    val notification: RuStoreNotification,
    val data: Map<String, String> = emptyMap(),
    val android: RuStoreAndroid? = null,
)

@Serializable
private data class RuStoreNotification(
    val title: String,
    val body: String,
)

@Serializable
private data class RuStoreAndroid(
    val notification: RuStoreAndroidNotification,
)

@Serializable
private data class RuStoreAndroidNotification(
    val title: String? = null,
    val body: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("click_action") val clickAction: String,
    @SerialName("click_action_type") val clickActionType: Int = 1,
)

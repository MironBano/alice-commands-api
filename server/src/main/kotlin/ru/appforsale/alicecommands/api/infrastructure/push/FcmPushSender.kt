package ru.appforsale.alicecommands.api.infrastructure.push

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference

interface FcmPushSender {
    fun send(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>,
        channelId: String?,
        deeplink: String,
    ): Result<Unit>
}

class NoOpFcmPushSender : FcmPushSender {
    private val log = LoggerFactory.getLogger(NoOpFcmPushSender::class.java)

    override fun send(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>,
        channelId: String?,
        deeplink: String,
    ): Result<Unit> {
        log.info(
            "dry-run fcm push scenario={} tokenTail={} title={} deeplink={}",
            data["scenario"],
            token.takeLast(8),
            title,
            deeplink,
        )
        return Result.success(Unit)
    }
}

/**
 * FCM HTTP v1 with a service-account JWT.
 * Env: FCM_PROJECT_ID + FCM_SERVICE_ACCOUNT_JSON (file path or inline JSON).
 */
class HttpFcmPushSender(
    private val projectId: String,
    private val serviceAccountJson: String,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) : FcmPushSender {

    private val log = LoggerFactory.getLogger(HttpFcmPushSender::class.java)
    private val cachedToken = AtomicReference<CachedAccessToken?>(null)
    private val account: ServiceAccount = parseServiceAccount(serviceAccountJson)

    override fun send(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>,
        channelId: String?,
        deeplink: String,
    ): Result<Unit> = runCatching {
        val accessToken = accessToken()
        val url = URI("https://fcm.googleapis.com/v1/projects/$projectId/messages:send").toURL()
        val payload = buildFcmSendPayloadJson(
            json = json,
            token = token,
            title = title,
            body = body,
            data = data,
            channelId = channelId,
            deeplink = deeplink,
        )
        val bodyBytes = payload.toByteArray(StandardCharsets.UTF_8)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $accessToken")
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
            log.warn("FCM push failed status={} body={}", code, responseText.take(500))
            error("fcm_push_http_$code")
        }
    }

    private fun accessToken(): String {
        val now = Instant.now().epochSecond
        cachedToken.get()?.takeIf { it.expiresAtEpochSec > now + 60 }?.let { return it.value }
        val jwt = signJwt(now)
        val form = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=$jwt"
        val url = URI("https://oauth2.googleapis.com/token").toURL()
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connectTimeout = 10_000
            readTimeout = 15_000
        }
        connection.outputStream.use { it.write(form.toByteArray(StandardCharsets.UTF_8)) }
        val code = connection.responseCode
        val responseText = runCatching {
            (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.readText().orEmpty()
        }.getOrDefault("")
        if (code !in 200..299) {
            error("fcm_oauth_http_$code: ${responseText.take(200)}")
        }
        val tokenResponse = json.decodeFromString(OAuthTokenResponse.serializer(), responseText)
        val expiresAt = now + tokenResponse.expiresIn.coerceAtLeast(60)
        cachedToken.set(CachedAccessToken(tokenResponse.accessToken, expiresAt))
        return tokenResponse.accessToken
    }

    private fun signJwt(nowEpochSec: Long): String {
        val header = base64Url("""{"alg":"RS256","typ":"JWT"}""")
        val claim = base64Url(
            """{"iss":"${account.clientEmail}","scope":"https://www.googleapis.com/auth/firebase.messaging","aud":"https://oauth2.googleapis.com/token","iat":$nowEpochSec,"exp":${nowEpochSec + 3600}}""",
        )
        val signingInput = "$header.$claim"
        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(account.privateKey)
            update(signingInput.toByteArray(StandardCharsets.UTF_8))
        }.sign()
        return "$signingInput.${base64Url(signature)}"
    }

    private fun parseServiceAccount(raw: String): ServiceAccount {
        val jsonText = resolveServiceAccountJson(raw)
        val dto = json.decodeFromString(ServiceAccountDto.serializer(), jsonText)
        require(dto.clientEmail.isNotBlank()) { "FCM service account missing client_email" }
        require(dto.privateKey.isNotBlank()) { "FCM service account missing private_key" }
        val pem = dto.privateKey
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\n", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
        val keyBytes = Base64.getDecoder().decode(pem)
        val privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(keyBytes))
        return ServiceAccount(dto.clientEmail, privateKey)
    }

    private data class CachedAccessToken(val value: String, val expiresAtEpochSec: Long)
    private data class ServiceAccount(val clientEmail: String, val privateKey: java.security.PrivateKey)

    @Serializable
    private data class ServiceAccountDto(
        @SerialName("client_email") val clientEmail: String = "",
        @SerialName("private_key") val privateKey: String = "",
    )

    @Serializable
    private data class OAuthTokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("expires_in") val expiresIn: Long = 3600,
    )
}

internal fun resolveServiceAccountJson(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.startsWith("{")) return trimmed
    val path = Path.of(trimmed)
    require(Files.isRegularFile(path)) { "FCM_SERVICE_ACCOUNT_JSON file not found: $trimmed" }
    return Files.readString(path)
}

internal fun buildFcmSendPayloadJson(
    json: Json,
    token: String,
    title: String,
    body: String,
    data: Map<String, String>,
    channelId: String?,
    deeplink: String,
): String {
    val dataFields = data.toMutableMap()
    if (deeplink.isNotBlank()) {
        dataFields.putIfAbsent("deeplink", deeplink)
    }
    val payload = FcmSendRequest(
        message = FcmMessage(
            token = token,
            notification = FcmNotification(title = title, body = body),
            data = dataFields,
            android = FcmAndroidConfig(
                priority = "HIGH",
                notification = FcmAndroidNotification(
                    channelId = channelId?.takeIf { it.isNotBlank() } ?: DEFAULT_RUSTORE_PUSH_CHANNEL_ID,
                    // Do NOT set click_action to a deeplink URI — FCM treats it as Intent action.
                    // Deeplink travels in data map; MainActivity reads extras["deeplink"].
                ),
            ),
        ),
    )
    return json.encodeToString(FcmSendRequest.serializer(), payload)
}

@Serializable
private data class FcmSendRequest(
    @SerialName("message") val message: FcmMessage,
)

@Serializable
private data class FcmMessage(
    @SerialName("token") val token: String,
    @SerialName("notification") val notification: FcmNotification,
    @SerialName("data") val data: Map<String, String> = emptyMap(),
    @SerialName("android") val android: FcmAndroidConfig? = null,
)

@Serializable
private data class FcmNotification(
    @SerialName("title") val title: String,
    @SerialName("body") val body: String,
)

@Serializable
private data class FcmAndroidConfig(
    @SerialName("priority") val priority: String,
    @SerialName("notification") val notification: FcmAndroidNotification,
)

@Serializable
private data class FcmAndroidNotification(
    @SerialName("channel_id") val channelId: String,
)

private fun base64Url(raw: String): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray(StandardCharsets.UTF_8))

private fun base64Url(raw: ByteArray): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(raw)

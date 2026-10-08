package ru.appforsale.alicecommands.api.infrastructure.push

import ru.appforsale.alicecommands.api.domain.push.PushProvider

/**
 * Routes campaign delivery by push provider stored on the token row.
 */
class ProviderAwarePushSender(
    private val rustore: RuStorePushSender,
    private val fcm: FcmPushSender,
) {
    fun send(
        provider: String,
        token: String,
        title: String,
        body: String,
        data: Map<String, String>,
        channelId: String?,
        deeplink: String,
    ): Result<Unit> =
        when (PushProvider.normalize(provider)) {
            PushProvider.FCM -> fcm.send(token, title, body, data, channelId, deeplink)
            else -> rustore.send(token, title, body, data, channelId, deeplink)
        }
}

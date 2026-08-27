package ru.appforsale.alicecommands.api.application.publish

import ru.appforsale.alicecommands.api.domain.DeviceGuide
import ru.appforsale.alicecommands.api.domain.DevicePick

private const val DETAIL_PLACEMENT = "device_guide_detail"

fun canonicalPickId(guideId: String): String = "pick_$guideId"

fun resolveDetailReferralPickIds(
    guide: DeviceGuide,
    picksById: Map<String, DevicePick>,
): List<String> {
    val primaryId = canonicalPickId(guide.id)
    val ordered = mutableListOf<String>()

    picksById[primaryId]?.let { ordered += primaryId }

    guide.related_device_ids.forEach { relatedGuideId ->
        val relatedPickId = canonicalPickId(relatedGuideId)
        if (relatedPickId == primaryId || relatedPickId in ordered) return@forEach
        val pick = picksById[relatedPickId] ?: return@forEach
        if (DETAIL_PLACEMENT !in pick.placements) return@forEach
        ordered += relatedPickId
    }

    return ordered
}

fun enrichGuidesWithDetailReferralPickIds(
    guides: List<DeviceGuide>,
    picks: List<DevicePick>,
): List<DeviceGuide> {
    val picksById = picks.associateBy { it.id }
    return guides.map { guide ->
        guide.copy(
            detail_referral_pick_ids = resolveDetailReferralPickIds(guide, picksById),
        )
    }
}

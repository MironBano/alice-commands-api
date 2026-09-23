package ru.appforsale.alicecommands.api.domain.ports

import ru.appforsale.alicecommands.api.domain.Announcement
import java.time.OffsetDateTime

interface AnnouncementsRepository {
    fun listAll(): List<Announcement>
    fun listActive(now: OffsetDateTime): List<Announcement>
    fun getById(id: String): Announcement?
    fun create(announcement: Announcement): Announcement
    fun update(announcement: Announcement): Announcement
    fun delete(id: String): Boolean
    fun maxUpdatedAt(): OffsetDateTime?
    fun maxActiveUpdatedAt(now: OffsetDateTime): OffsetDateTime?
}

interface AnnouncementImageStorage {
    fun store(slug: String, bytes: ByteArray, extension: String): String
    fun imageUrl(slug: String, extension: String): String
    fun basePublicUrl(): String
    fun exists(slug: String, extension: String): Boolean
    fun isWritable(): Boolean
}

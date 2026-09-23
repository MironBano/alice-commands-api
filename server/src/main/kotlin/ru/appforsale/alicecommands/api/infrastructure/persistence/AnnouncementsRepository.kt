package ru.appforsale.alicecommands.api.infrastructure.persistence

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import ru.appforsale.alicecommands.api.domain.Announcement
import ru.appforsale.alicecommands.api.domain.ports.AnnouncementsRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ExposedAnnouncementsRepository(
    private val database: Database,
) : AnnouncementsRepository {

    override fun listAll(): List<Announcement> = transaction(database) {
        AnnouncementsTable
            .selectAll()
            .orderBy(AnnouncementsTable.priority to SortOrder.DESC, AnnouncementsTable.updatedAt to SortOrder.DESC)
            .map { rowToAnnouncement(it) }
    }

    override fun listActive(now: OffsetDateTime): List<Announcement> = transaction(database) {
        AnnouncementsTable
            .selectAll()
            .where {
                (AnnouncementsTable.enabled eq true) and
                    (
                        AnnouncementsTable.startsAt.isNull() or
                            (AnnouncementsTable.startsAt lessEq now)
                        ) and
                    (
                        AnnouncementsTable.endsAt.isNull() or
                            (AnnouncementsTable.endsAt greaterEq now)
                        )
            }
            .orderBy(AnnouncementsTable.priority to SortOrder.DESC, AnnouncementsTable.updatedAt to SortOrder.DESC)
            .map { rowToAnnouncement(it) }
    }

    override fun getById(id: String): Announcement? = transaction(database) {
        AnnouncementsTable
            .selectAll()
            .where { AnnouncementsTable.id eq id }
            .singleOrNull()
            ?.let { rowToAnnouncement(it) }
    }

    override fun create(announcement: Announcement): Announcement = transaction(database) {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        AnnouncementsTable.insert {
            it[id] = announcement.id
            it[revision] = announcement.revision
            it[placement] = announcement.placement
            it[title] = announcement.title
            it[body] = announcement.body
            it[imageUrl] = announcement.image_url
            it[backgroundColor] = announcement.background_color
            it[foregroundColor] = announcement.foreground_color
            it[ctaLabel] = announcement.cta_label
            it[ctaAction] = announcement.cta_action
            it[ctaTarget] = announcement.cta_target
            it[dismissible] = announcement.dismissible
            it[priority] = announcement.priority
            it[enabled] = announcement.enabled
            it[minAppVersion] = announcement.min_app_version
            it[maxAppVersion] = announcement.max_app_version
            it[startsAt] = parseInstant(announcement.starts_at)
            it[endsAt] = parseInstant(announcement.ends_at)
            it[createdAt] = now
            it[updatedAt] = now
        }
        getById(announcement.id)!!
    }

    override fun update(announcement: Announcement): Announcement = transaction(database) {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val existing = getById(announcement.id)
            ?: error("announcement not found: ${announcement.id}")
        val nextRevision = existing.revision + 1
        AnnouncementsTable.update({ AnnouncementsTable.id eq announcement.id }) {
            it[revision] = nextRevision
            it[placement] = announcement.placement
            it[title] = announcement.title
            it[body] = announcement.body
            it[imageUrl] = announcement.image_url
            it[backgroundColor] = announcement.background_color
            it[foregroundColor] = announcement.foreground_color
            it[ctaLabel] = announcement.cta_label
            it[ctaAction] = announcement.cta_action
            it[ctaTarget] = announcement.cta_target
            it[dismissible] = announcement.dismissible
            it[priority] = announcement.priority
            it[enabled] = announcement.enabled
            it[minAppVersion] = announcement.min_app_version
            it[maxAppVersion] = announcement.max_app_version
            it[startsAt] = parseInstant(announcement.starts_at)
            it[endsAt] = parseInstant(announcement.ends_at)
            it[updatedAt] = now
        }
        getById(announcement.id)!!
    }

    override fun delete(id: String): Boolean = transaction(database) {
        AnnouncementsTable.deleteWhere { AnnouncementsTable.id eq id } > 0
    }

    override fun maxUpdatedAt(): OffsetDateTime? = transaction(database) {
        AnnouncementsTable
            .selectAll()
            .maxByOrNull { it[AnnouncementsTable.updatedAt] }
            ?.get(AnnouncementsTable.updatedAt)
    }

    override fun maxActiveUpdatedAt(now: OffsetDateTime): OffsetDateTime? = transaction(database) {
        AnnouncementsTable
            .selectAll()
            .where {
                (AnnouncementsTable.enabled eq true) and
                    (
                        AnnouncementsTable.startsAt.isNull() or
                            (AnnouncementsTable.startsAt lessEq now)
                        ) and
                    (
                        AnnouncementsTable.endsAt.isNull() or
                            (AnnouncementsTable.endsAt greaterEq now)
                        )
            }
            .maxByOrNull { it[AnnouncementsTable.updatedAt] }
            ?.get(AnnouncementsTable.updatedAt)
    }

    private fun rowToAnnouncement(row: org.jetbrains.exposed.sql.ResultRow): Announcement =
        Announcement(
            id = row[AnnouncementsTable.id],
            revision = row[AnnouncementsTable.revision],
            placement = row[AnnouncementsTable.placement],
            title = row[AnnouncementsTable.title],
            body = row[AnnouncementsTable.body],
            image_url = row[AnnouncementsTable.imageUrl],
            background_color = row[AnnouncementsTable.backgroundColor],
            foreground_color = row[AnnouncementsTable.foregroundColor],
            cta_label = row[AnnouncementsTable.ctaLabel],
            cta_action = row[AnnouncementsTable.ctaAction],
            cta_target = row[AnnouncementsTable.ctaTarget],
            dismissible = row[AnnouncementsTable.dismissible],
            priority = row[AnnouncementsTable.priority],
            enabled = row[AnnouncementsTable.enabled],
            min_app_version = row[AnnouncementsTable.minAppVersion],
            max_app_version = row[AnnouncementsTable.maxAppVersion],
            starts_at = row[AnnouncementsTable.startsAt]?.format(ISO_FORMAT),
            ends_at = row[AnnouncementsTable.endsAt]?.format(ISO_FORMAT),
            created_at = row[AnnouncementsTable.createdAt].format(ISO_FORMAT),
            updated_at = row[AnnouncementsTable.updatedAt].format(ISO_FORMAT),
        )

    private fun parseInstant(raw: String?): OffsetDateTime? {
        if (raw.isNullOrBlank()) return null
        return OffsetDateTime.parse(raw.trim())
    }

    companion object {
        private val ISO_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    }
}

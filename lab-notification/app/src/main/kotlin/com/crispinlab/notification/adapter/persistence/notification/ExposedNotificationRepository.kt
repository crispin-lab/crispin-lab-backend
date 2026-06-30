package com.crispinlab.notification.adapter.persistence.notification

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.persistence.ExposedEntityRepository
import com.crispinlab.notification.adapter.persistence.toPageResult
import com.crispinlab.notification.application.port.outgoing.notification.NotificationRepository
import com.crispinlab.notification.domain.notification.Notification
import com.crispinlab.notification.domain.notification.NotificationId
import com.crispinlab.notification.domain.notification.NotificationType
import com.crispinlab.notification.domain.notification.NotificationType.Companion.asNotificationType
import com.crispinlab.notification.domain.notification.SourceType
import com.crispinlab.notification.domain.notification.SourceType.Companion.asSourceType
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedNotificationRepository :
    ExposedEntityRepository<Notification, NotificationId>(),
    NotificationRepository {
    override val table = Notifications
    override val idColumn = Notifications.id
    override val deletedAtColumn = null
    override val updateExclude =
        listOf(
            Notifications.id,
            Notifications.userId,
            Notifications.type,
            Notifications.sourceType,
            Notifications.sourceId,
            Notifications.actorUserId,
            Notifications.createdAt
        )

    override fun ResultRow.toEntity(): Notification =
        Notification(
            id = NotificationId(this[Notifications.id]),
            userId = UserId(this[Notifications.userId]),
            type = decodeType(this[Notifications.type]),
            sourceType = decodeSourceType(this[Notifications.sourceType]),
            sourceId = this[Notifications.sourceId],
            actorUserId = UserId(this[Notifications.actorUserId]),
            isRead = this[Notifications.isRead],
            readAt = this[Notifications.readAt],
            createdAt = this[Notifications.createdAt]
        )

    override fun upsertBody(
        builder: UpsertStatement<Long>,
        entity: Notification
    ) {
        builder[Notifications.id] = entity.id.value
        builder[Notifications.userId] = entity.userId.value
        builder[Notifications.type] = entity.type.name
        builder[Notifications.sourceType] = entity.sourceType.name
        builder[Notifications.sourceId] = entity.sourceId
        builder[Notifications.actorUserId] = entity.actorUserId.value
        builder[Notifications.isRead] = entity.isRead
        builder[Notifications.readAt] = entity.readAt
        builder[Notifications.createdAt] = entity.createdAt
    }

    override fun saveAll(entities: List<Notification>): List<Notification> {
        if (entities.isEmpty()) return entities
        Notifications.batchInsert(entities, ignore = true) { entity ->
            this[Notifications.id] = entity.id.value
            this[Notifications.userId] = entity.userId.value
            this[Notifications.type] = entity.type.name
            this[Notifications.sourceType] = entity.sourceType.name
            this[Notifications.sourceId] = entity.sourceId
            this[Notifications.actorUserId] = entity.actorUserId.value
            this[Notifications.isRead] = entity.isRead
            this[Notifications.readAt] = entity.readAt
            this[Notifications.createdAt] = entity.createdAt
        }
        return entities
    }

    override fun search(
        userId: UserId,
        unreadOnly: Boolean,
        pageRequest: PageRequest
    ): PageResult<Notification> =
        Notifications
            .selectAll()
            .where {
                val base = Notifications.userId eq userId.value
                if (unreadOnly) base and (Notifications.isRead eq false) else base
            }.toPageResult(
                pageRequest,
                Notifications.createdAt to SortOrder.DESC,
                Notifications.id to SortOrder.DESC
            ) { it.toEntity() }

    override fun existsBy(
        userId: UserId,
        type: NotificationType,
        sourceType: SourceType,
        sourceId: Long
    ): Boolean =
        Notifications
            .select(Notifications.id)
            .where {
                (Notifications.userId eq userId.value) and
                    (Notifications.type eq type.name) and
                    (Notifications.sourceType eq sourceType.name) and
                    (Notifications.sourceId eq sourceId)
            }.limit(1)
            .any()

    private fun decodeType(stored: String): NotificationType =
        runCatching { stored.asNotificationType() }
            .getOrElse { cause ->
                throw IllegalStateException("저장된 알림 타입을 해석할 수 없습니다.", cause)
            }

    private fun decodeSourceType(stored: String): SourceType =
        runCatching { stored.asSourceType() }
            .getOrElse { cause ->
                throw IllegalStateException("저장된 source type 을 해석할 수 없습니다.", cause)
            }
}

package com.crispinlab.notification.application.port.outgoing.notification

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.notification.domain.notification.Notification
import com.crispinlab.notification.domain.notification.NotificationId
import com.crispinlab.notification.domain.notification.NotificationType
import com.crispinlab.notification.domain.notification.SourceType
import com.crispinlab.user.domain.user.UserId

interface NotificationRepository {
    fun save(entity: Notification): Notification

    fun saveAll(entities: List<Notification>): List<Notification>

    fun findBy(id: NotificationId): Notification?

    fun search(
        userId: UserId,
        unreadOnly: Boolean,
        pageRequest: PageRequest
    ): PageResult<Notification>

    fun existsBy(
        userId: UserId,
        type: NotificationType,
        sourceType: SourceType,
        sourceId: Long
    ): Boolean

    fun existingUserIdsAmong(
        userIds: Collection<UserId>,
        type: NotificationType,
        sourceType: SourceType,
        sourceId: Long
    ): Set<UserId>
}

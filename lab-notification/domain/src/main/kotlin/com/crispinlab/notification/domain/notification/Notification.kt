package com.crispinlab.notification.domain.notification

import com.crispinlab.common.domain.Entity
import com.crispinlab.user.domain.user.UserId
import java.time.Instant
import java.time.Instant.now

class Notification(
    override val id: NotificationId,
    val userId: UserId,
    val type: NotificationType,
    val sourceType: SourceType,
    val sourceId: Long,
    val actorUserId: UserId,
    val createdAt: Instant = now(),
    isRead: Boolean = false,
    readAt: Instant? = null
) : Entity<NotificationId> {
    var isRead: Boolean = isRead
        private set
    var readAt: Instant? = readAt
        private set

    fun markAsRead() {
        if (isRead) return
        isRead = true
        readAt = now()
    }
}

package com.crispinlab.notification.application.port.incoming.notification

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.notification.application.port.incoming.notification.NotificationListing.Request
import com.crispinlab.notification.application.port.incoming.notification.NotificationListing.Summary
import com.crispinlab.notification.domain.access.Viewer
import com.crispinlab.notification.domain.notification.NotificationId
import com.crispinlab.notification.domain.notification.NotificationType
import com.crispinlab.notification.domain.notification.SourceType
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface NotificationListing : UseCase<Request, PageResult<Summary>> {
    class Request(
        page: Int = 0,
        size: Int = DEFAULT_SIZE,
        val unreadOnly: Boolean = false,
        val viewer: Viewer.Member
    ) {
        val pageRequest: PageRequest =
            PageRequest(
                page = page,
                size = size
            )
    }

    data class Summary(
        val notificationId: NotificationId,
        val type: NotificationType,
        val sourceType: SourceType,
        val sourceId: Long,
        val actorUserId: UserId,
        val isRead: Boolean,
        val createdAt: Instant,
        val readAt: Instant?
    )
}

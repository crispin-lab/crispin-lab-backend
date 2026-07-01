package com.crispinlab.notification.application.usecase.notification

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.notification.application.port.incoming.notification.NotificationListing
import com.crispinlab.notification.application.port.incoming.notification.NotificationListing.Request
import com.crispinlab.notification.application.port.incoming.notification.NotificationListing.Summary
import com.crispinlab.notification.application.port.outgoing.notification.NotificationRepository
import com.crispinlab.notification.domain.notification.Notification
import org.springframework.stereotype.Service

@Service
class NotificationListingUseCase(
    private val notificationRepository: NotificationRepository,
    private val transactionProvider: TransactionProvider
) : NotificationListing {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            notificationRepository
                .search(
                    userId = request.viewer.userId,
                    unreadOnly = request.unreadOnly,
                    pageRequest = request.pageRequest
                ).map { it.toSummary() }
        }

    private fun Notification.toSummary(): Summary =
        Summary(
            notificationId = id,
            type = type,
            sourceType = sourceType,
            sourceId = sourceId,
            actorUserId = actorUserId,
            isRead = isRead,
            createdAt = createdAt,
            readAt = readAt
        )
}

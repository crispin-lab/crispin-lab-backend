package com.crispinlab.notification.application.usecase.notification

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.notification.application.port.incoming.notification.NotificationReading
import com.crispinlab.notification.application.port.incoming.notification.NotificationReading.Request
import com.crispinlab.notification.application.port.outgoing.notification.NotificationRepository
import com.crispinlab.notification.domain.notification.Notification
import com.crispinlab.notification.domain.notification.NotificationErrorCode
import org.springframework.stereotype.Service

@Service
class NotificationReadingUseCase(
    private val notificationRepository: NotificationRepository,
    private val transactionProvider: TransactionProvider
) : NotificationReading {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            request
                .toEntity()
                .also { it.markAsRead() }
                .let { notificationRepository.save(it) }
        }
    }

    private fun Request.toEntity(): Notification =
        notificationRepository
            .findBy(notificationId)
            ?.takeIf { it.userId == viewer.userId }
            ?: throw NotFoundException(NotificationErrorCode.NOTIFICATION_NOT_FOUND)
}

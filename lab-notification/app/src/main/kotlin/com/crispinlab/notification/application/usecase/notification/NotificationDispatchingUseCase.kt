package com.crispinlab.notification.application.usecase.notification

import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.notification.application.port.incoming.notification.NotificationDispatching
import com.crispinlab.notification.application.port.incoming.notification.NotificationDispatching.Request
import com.crispinlab.notification.application.port.outgoing.notification.NotificationRepository
import com.crispinlab.notification.domain.notification.Notification
import com.crispinlab.notification.domain.notification.NotificationId
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Service

@Service
class NotificationDispatchingUseCase(
    private val notificationRepository: NotificationRepository,
    private val idGenerator: IdGenerator,
    private val transactionProvider: TransactionProvider
) : NotificationDispatching {
    override fun perform(request: Request) {
        if (request.targetUserIds.isEmpty()) return
        transactionProvider.transactional {
            request.targetUserIds
                .filterNot { it.alreadyNotifiedFor(request) }
                .map { it.toNotification(request) }
                .let { notificationRepository.saveAll(it) }
        }
    }

    private fun UserId.alreadyNotifiedFor(request: Request): Boolean =
        notificationRepository.existsBy(
            userId = this,
            type = request.type,
            sourceType = request.sourceType,
            sourceId = request.sourceId
        )

    private fun UserId.toNotification(request: Request): Notification =
        Notification(
            id = NotificationId(idGenerator.next()),
            userId = this,
            type = request.type,
            sourceType = request.sourceType,
            sourceId = request.sourceId,
            actorUserId = request.actorUserId
        )
}

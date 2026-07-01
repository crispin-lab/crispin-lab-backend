package com.crispinlab.notification.application.port.incoming.notification

import com.crispinlab.common.application.UseCase
import com.crispinlab.notification.application.port.incoming.notification.NotificationDispatching.Request
import com.crispinlab.notification.domain.notification.NotificationType
import com.crispinlab.notification.domain.notification.SourceType
import com.crispinlab.user.domain.user.UserId

interface NotificationDispatching : UseCase<Request, Unit> {
    class Request(
        val sourceType: SourceType,
        val sourceId: Long,
        val type: NotificationType,
        val targetUserIds: List<UserId>,
        val actorUserId: UserId
    )
}

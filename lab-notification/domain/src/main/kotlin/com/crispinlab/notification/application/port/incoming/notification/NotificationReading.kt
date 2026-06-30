package com.crispinlab.notification.application.port.incoming.notification

import com.crispinlab.common.application.UseCase
import com.crispinlab.notification.application.port.incoming.notification.NotificationReading.Request
import com.crispinlab.notification.domain.access.Viewer
import com.crispinlab.notification.domain.notification.NotificationId
import com.crispinlab.notification.domain.notification.NotificationId.Companion.asNotificationId

interface NotificationReading : UseCase<Request, Unit> {
    class Request(
        notificationId: String,
        val viewer: Viewer.Member
    ) {
        val notificationId: NotificationId = notificationId.asNotificationId()
    }
}

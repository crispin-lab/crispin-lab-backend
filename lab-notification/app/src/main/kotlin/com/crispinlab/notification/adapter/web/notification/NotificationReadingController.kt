package com.crispinlab.notification.adapter.web.notification

import com.crispinlab.notification.adapter.web.auth.toMember
import com.crispinlab.notification.application.port.incoming.notification.NotificationReading
import com.crispinlab.notification.application.port.incoming.notification.NotificationReading.Request
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/notifications/{notificationId}/read")
class NotificationReadingController(
    private val useCase: NotificationReading
) {
    @PostMapping
    fun read(
        @PathVariable notificationId: String,
        auth: Auth
    ) {
        Request(
            notificationId = notificationId,
            viewer = auth.toMember()
        ).let { useCase.perform(it) }
    }
}

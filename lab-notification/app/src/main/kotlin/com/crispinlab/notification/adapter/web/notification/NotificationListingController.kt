package com.crispinlab.notification.adapter.web.notification

import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.notification.adapter.web.auth.toMember
import com.crispinlab.notification.application.port.incoming.notification.NotificationListing
import com.crispinlab.notification.application.port.incoming.notification.NotificationListing.Request
import com.crispinlab.notification.application.port.incoming.notification.NotificationListing.Summary
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/notifications")
class NotificationListingController(
    private val useCase: NotificationListing
) {
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_SIZE") size: Int,
        @RequestParam(defaultValue = "false") unreadOnly: Boolean,
        auth: Auth
    ): PageResult<Summary> =
        Request(
            page = page,
            size = size,
            unreadOnly = unreadOnly,
            viewer = auth.toMember()
        ).let { useCase.perform(it) }
}

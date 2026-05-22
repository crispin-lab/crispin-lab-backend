package com.crispinlab.space.adapter.web.page

import com.crispinlab.space.application.port.incoming.page.PageGetting
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.incoming.page.PageGetting.Result
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}")
class PageGettingController(
    private val useCase: PageGetting
) {
    @GetMapping
    fun get(
        @PathVariable pageId: String,
        auth: Auth?
    ): Result =
        Request(
            pageId = pageId,
            currentUserId = auth?.userId,
            currentUserRole = auth?.role
        ).let {
            useCase.perform(it)
        }
}

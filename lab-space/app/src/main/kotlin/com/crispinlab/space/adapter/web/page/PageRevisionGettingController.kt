package com.crispinlab.space.adapter.web.page

import com.crispinlab.space.adapter.web.auth.toViewer
import com.crispinlab.space.application.port.incoming.page.PageRevisionGetting
import com.crispinlab.space.application.port.incoming.page.PageRevisionGetting.Request
import com.crispinlab.space.application.port.incoming.page.PageRevisionGetting.Result
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/revisions/{version}")
class PageRevisionGettingController(
    private val useCase: PageRevisionGetting
) {
    @GetMapping
    fun get(
        @PathVariable pageId: String,
        @PathVariable version: Int,
        auth: Auth?
    ): Result =
        Request(
            pageId = pageId,
            version = version,
            viewer = auth.toViewer()
        ).let {
            useCase.perform(it)
        }
}

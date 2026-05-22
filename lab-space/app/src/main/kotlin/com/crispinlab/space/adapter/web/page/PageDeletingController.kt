package com.crispinlab.space.adapter.web.page

import com.crispinlab.space.adapter.web.auth.toViewer
import com.crispinlab.space.application.port.incoming.page.PageDeleting
import com.crispinlab.space.application.port.incoming.page.PageDeleting.Request
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}")
class PageDeletingController(
    private val useCase: PageDeleting
) {
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable pageId: String,
        auth: Auth
    ) {
        Request(pageId = pageId, viewer = auth.toViewer())
            .let {
                useCase.perform(it)
            }
    }
}

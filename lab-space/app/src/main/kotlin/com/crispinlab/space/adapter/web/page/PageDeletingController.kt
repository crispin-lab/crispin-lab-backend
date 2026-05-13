package com.crispinlab.space.adapter.web.page

import com.crispinlab.space.adapter.web.auth.Auth
import com.crispinlab.space.application.port.incoming.page.PageDeleting
import com.crispinlab.space.application.port.incoming.page.PageDeleting.Request
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
        Request(pageId = pageId, currentUserId = auth.userId)
            .let { useCase.perform(it) }
    }
}

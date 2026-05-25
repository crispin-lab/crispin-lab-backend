package com.crispinlab.space.adapter.web.tag

import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.tag.PageTagDetaching
import com.crispinlab.space.application.port.incoming.tag.PageTagDetaching.Request
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/tags/{tagId}")
class PageTagDetachingController(
    private val useCase: PageTagDetaching
) {
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun detach(
        @PathVariable pageId: String,
        @PathVariable tagId: String,
        auth: Auth
    ) {
        Request(
            pageId = pageId,
            tagId = tagId,
            viewer = auth.toMember()
        ).let {
            useCase.perform(it)
        }
    }
}

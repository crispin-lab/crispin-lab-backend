package com.crispinlab.space.adapter.web.tag

import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.tag.PageTagAttaching
import com.crispinlab.space.application.port.incoming.tag.PageTagAttaching.Request
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/tags")
class PageTagAttachingController(
    private val useCase: PageTagAttaching
) {
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun attach(
        @PathVariable pageId: String,
        @RequestBody body: Body,
        auth: Auth
    ) {
        body
            .toRequestWith(
                pageId = pageId,
                viewer = auth.toMember()
            ).let {
                useCase.perform(it)
            }
    }

    data class Body(
        val tagId: String
    ) {
        fun toRequestWith(
            pageId: String,
            viewer: Viewer.Member
        ): Request =
            Request(
                pageId = pageId,
                tagId = tagId,
                viewer = viewer
            )
    }
}

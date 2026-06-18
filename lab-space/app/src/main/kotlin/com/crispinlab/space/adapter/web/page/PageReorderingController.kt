package com.crispinlab.space.adapter.web.page

import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.page.PageReordering
import com.crispinlab.space.application.port.incoming.page.PageReordering.Request
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.user.adapter.web.auth.Auth
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}")
class PageReorderingController(
    private val useCase: PageReordering
) {
    @PutMapping("/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun reorder(
        @PathVariable pageId: String,
        @RequestBody body: Body,
        auth: Auth
    ) {
        body
            .toRequestWith(pageId = pageId, viewer = auth.toMember())
            .let {
                useCase.perform(it)
            }
    }

    data class Body(
        val displayOrder: Int
    ) {
        fun toRequestWith(
            pageId: String,
            viewer: Viewer.Member
        ): Request =
            Request(
                pageId = pageId,
                displayOrder = displayOrder,
                viewer = viewer
            )
    }
}

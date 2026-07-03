package com.crispinlab.composition.adapter.web.comment

import com.crispinlab.composition.application.port.incoming.comment.CommentRegisteringComposition
import com.crispinlab.composition.application.port.incoming.comment.CommentRegisteringComposition.Request
import com.crispinlab.composition.application.port.incoming.comment.CommentRegisteringComposition.Result
import com.crispinlab.space.adapter.web.auth.toMember
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
@RequestMapping("/v1/pages/{pageId}/comments")
class CommentRegisteringCompositionController(
    private val useCase: CommentRegisteringComposition
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @PathVariable pageId: String,
        @RequestBody body: Body,
        auth: Auth
    ): Result =
        body
            .toRequestWith(
                pageId = pageId,
                viewer = auth.toMember()
            ).let { useCase.perform(it) }

    data class Body(
        val content: String
    ) {
        fun toRequestWith(
            pageId: String,
            viewer: Viewer.Member
        ): Request =
            Request(
                pageId = pageId,
                content = content,
                viewer = viewer
            )
    }
}

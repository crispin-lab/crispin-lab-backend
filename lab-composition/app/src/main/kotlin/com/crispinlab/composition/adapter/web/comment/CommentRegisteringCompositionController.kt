package com.crispinlab.composition.adapter.web.comment

import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.composition.application.port.outgoing.user.handleOf
import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Request
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentId
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
    private val useCase: CommentRegistering,
    private val userHandleLookup: UserHandleLookup
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @PathVariable pageId: String,
        @RequestBody body: Body,
        auth: Auth
    ): CommentRegisterPayload =
        body
            .toRequestWith(
                pageId = pageId,
                viewer = auth.toMember()
            ).let {
                useCase.perform(it)
            }.toPayload()

    private fun Result.toPayload(): CommentRegisterPayload =
        CommentRegisterPayload(
            commentId = commentId,
            authorHandle = userHandleLookup.handleOf(authorId)
        )

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

    data class CommentRegisterPayload(
        val commentId: CommentId,
        val authorHandle: String
    )
}

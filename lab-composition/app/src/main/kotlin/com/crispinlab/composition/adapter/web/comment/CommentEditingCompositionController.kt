package com.crispinlab.composition.adapter.web.comment

import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.composition.application.port.outgoing.user.handleOf
import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.comment.CommentEditing
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Request
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.user.adapter.web.auth.Auth
import java.time.Instant
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/comments/{commentId}")
class CommentEditingCompositionController(
    private val useCase: CommentEditing,
    private val userHandleLookup: UserHandleLookup
) {
    @PutMapping
    fun edit(
        @PathVariable pageId: String,
        @PathVariable commentId: String,
        @RequestBody body: Body,
        auth: Auth
    ): CommentEditPayload =
        body
            .toRequestWith(
                pageId = pageId,
                commentId = commentId,
                viewer = auth.toMember()
            ).let {
                useCase.perform(it)
            }.toPayload()

    private fun Result.toPayload(): CommentEditPayload =
        CommentEditPayload(
            commentId = commentId,
            authorHandle = userHandleLookup.handleOf(authorId),
            content = content,
            updatedAt = updatedAt
        )

    data class Body(
        val content: String
    ) {
        fun toRequestWith(
            pageId: String,
            commentId: String,
            viewer: Viewer.Member
        ): Request =
            Request(
                pageId = pageId,
                commentId = commentId,
                content = content,
                viewer = viewer
            )
    }

    data class CommentEditPayload(
        val commentId: CommentId,
        val authorHandle: String,
        val content: String,
        val updatedAt: Instant
    )
}

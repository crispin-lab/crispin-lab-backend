package com.crispinlab.composition.adapter.web.comment

import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.composition.application.port.outgoing.user.handleOf
import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.comment.CommentGetting
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Request
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Result
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.domain.user.UserId
import java.time.Instant
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/comments/{commentId}")
class CommentGettingCompositionController(
    private val useCase: CommentGetting,
    private val userHandleLookup: UserHandleLookup
) {
    @GetMapping
    fun get(
        @PathVariable pageId: String,
        @PathVariable commentId: String,
        auth: Auth
    ): CommentPayload =
        Request(
            pageId = pageId,
            commentId = commentId,
            viewer = auth.toMember()
        ).let {
            useCase.perform(it)
        }.toPayload()

    private fun Result.toPayload(): CommentPayload =
        CommentPayload(
            commentId = commentId,
            pageId = pageId,
            authorId = authorId,
            authorHandle = userHandleLookup.handleOf(authorId),
            content = content,
            canEdit = canEdit,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    data class CommentPayload(
        val commentId: CommentId,
        val pageId: PageId,
        val authorId: UserId,
        val authorHandle: String,
        val content: String,
        val canEdit: Boolean,
        val createdAt: Instant,
        val updatedAt: Instant
    )
}

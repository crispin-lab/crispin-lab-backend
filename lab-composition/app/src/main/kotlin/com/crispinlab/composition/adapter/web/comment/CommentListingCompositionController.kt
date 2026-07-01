package com.crispinlab.composition.adapter.web.comment

import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.space.adapter.web.auth.toMember
import com.crispinlab.space.application.port.incoming.comment.CommentListing
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Request
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Summary
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.user.adapter.web.auth.Auth
import com.crispinlab.user.domain.user.UserId
import java.time.Instant
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/pages/{pageId}/comments")
class CommentListingCompositionController(
    private val useCase: CommentListing,
    private val userHandleLookup: UserHandleLookup
) {
    @GetMapping
    fun list(
        @PathVariable pageId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "$DEFAULT_SIZE") size: Int,
        auth: Auth
    ): PageResult<CommentSummary> =
        Request(
            pageId = pageId,
            page = page,
            size = size,
            viewer = auth.toMember()
        ).let { useCase.perform(it) }
            .toPayloads()

    private fun PageResult<Summary>.toPayloads(): PageResult<CommentSummary> {
        val handles = userHandleLookup.handlesOf(items.map { it.authorId }.toSet())
        return map { it.toSummary(handles) }
    }

    private fun Summary.toSummary(handles: Map<UserId, String>): CommentSummary =
        CommentSummary(
            commentId = commentId,
            pageId = pageId,
            authorId = authorId,
            authorHandle = handles[authorId] ?: "",
            content = content,
            canEdit = canEdit,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    data class CommentSummary(
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

package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentListing
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Request
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Summary
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.requireReadablePage
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import org.springframework.stereotype.Service

@Service
class CommentListingUseCase(
    private val commentRepository: CommentRepository,
    private val pageRepository: PageRepository,
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val userHandleQuery: UserHandleQuery,
    private val transactionProvider: TransactionProvider
) : CommentListing {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request
                .also {
                    it.validate()
                }.toResult()
        }

    private fun Request.validate() {
        requireReadablePage(pageRepository, spaceRepository, spaceMemberRepository, viewer, pageId)
    }

    private fun Request.toResult(): PageResult<Summary> =
        commentRepository
            .findByPageId(pageId, pageRequest)
            .let { comments ->
                val handles = userHandleQuery.handlesOf(comments.items.map { it.authorId }.toSet())
                comments.map { it.toSummary(handles[it.authorId]?.value ?: "") }
            }

    private fun Comment.toSummary(authorHandle: String): Summary =
        Summary(
            commentId = id,
            pageId = pageId,
            authorId = authorId,
            authorHandle = authorHandle,
            body = body,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

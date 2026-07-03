package com.crispinlab.composition.application.usecase.comment

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.comment.CommentListingComposition
import com.crispinlab.composition.application.port.incoming.comment.CommentListingComposition.Request
import com.crispinlab.composition.application.port.incoming.comment.CommentListingComposition.Result
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.space.application.port.incoming.comment.CommentListing
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Summary
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Service

@Service
class CommentListingCompositionUseCase(
    private val commentListing: CommentListing,
    private val userHandleLookup: UserHandleLookup,
    private val transactionProvider: TransactionProvider
) : CommentListingComposition {
    override fun perform(request: Request): PageResult<Result> =
        transactionProvider.transactional(readOnly = true) {
            request
                .toDomainRequest()
                .let { commentListing.perform(it) }
                .toResults()
        }

    private fun Request.toDomainRequest(): CommentListing.Request =
        CommentListing.Request(
            pageId = pageId,
            page = page,
            size = size,
            viewer = viewer
        )

    private fun PageResult<Summary>.toResults(): PageResult<Result> {
        val handles = userHandleLookup.handlesOf(items.map { it.authorId }.toSet())
        return map { it.toResult(handles) }
    }

    private fun Summary.toResult(handles: Map<UserId, String>): Result =
        Result(
            commentId = commentId,
            pageId = pageId,
            authorId = authorId,
            authorHandle = handles[authorId] ?: "",
            content = content,
            canEdit = canEdit,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

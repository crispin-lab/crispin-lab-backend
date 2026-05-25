package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentListing
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Request
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Summary
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.page.PageErrorCode
import org.springframework.stereotype.Service

@Service
class CommentListingUseCase(
    private val commentRepository: CommentRepository,
    private val pageRepository: PageRepository,
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
        val scope = VisibilityScope.of(viewer)
        pageRepository
            .findBy(pageId)
            ?.takeIf { scope.allows(it.visibility, it.authorId) }
            ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
    }

    private fun Request.toResult(): PageResult<Summary> =
        commentRepository
            .findByPageId(pageId, pageRequest)
            .map { it.toSummary() }

    private fun Comment.toSummary(): Summary =
        Summary(
            commentId = id,
            pageId = pageId,
            authorId = authorId,
            body = body,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

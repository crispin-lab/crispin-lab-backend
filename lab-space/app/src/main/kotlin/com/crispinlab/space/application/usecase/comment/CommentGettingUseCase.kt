package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentGetting
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Request
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Result
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentErrorCode
import com.crispinlab.space.domain.page.PageErrorCode
import org.springframework.stereotype.Service

@Service
class CommentGettingUseCase(
    private val commentRepository: CommentRepository,
    private val pageRepository: PageRepository,
    private val transactionProvider: TransactionProvider
) : CommentGetting {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .also {
                    it.validate()
                }.toEntity()
                .toResult()
        }

    private fun Request.validate() {
        val scope = VisibilityScope.of(viewer)
        pageRepository
            .findBy(pageId)
            ?.takeIf { scope.allows(it.visibility, it.authorId) }
            ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
    }

    private fun Request.toEntity(): Comment =
        commentRepository
            .findBy(commentId)
            ?.takeIf { it.pageId == pageId }
            ?: throw NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND)

    private fun Comment.toResult(): Result =
        Result(
            commentId = id,
            pageId = pageId,
            authorId = authorId,
            body = body,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

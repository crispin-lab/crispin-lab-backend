package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentDeleting
import com.crispinlab.space.application.port.incoming.comment.CommentDeleting.Request
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentErrorCode
import org.springframework.stereotype.Service

@Service
class CommentDeletingUseCase(
    private val commentRepository: CommentRepository,
    private val transactionProvider: TransactionProvider
) : CommentDeleting {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            request
                .toEntity()
                .withdraw()
        }
    }

    private fun Request.toEntity(): Comment =
        commentRepository
            .findBy(commentId)
            ?.takeIf { it.pageId == pageId }
            ?.takeIf { viewer.isAdmin || it.authorId == viewer.userId }
            ?: throw NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND)

    private fun Comment.withdraw() {
        commentRepository.delete(id)
    }
}

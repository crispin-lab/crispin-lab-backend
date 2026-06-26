package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentEditing
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Request
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Result
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentErrorCode
import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import org.springframework.stereotype.Service

@Service
class CommentEditingUseCase(
    private val commentRepository: CommentRepository,
    private val userHandleQuery: UserHandleQuery,
    private val transactionProvider: TransactionProvider
) : CommentEditing {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .toEntity()
                .editWith(request)
                .let {
                    commentRepository.save(it)
                }.toResult()
        }

    private fun Request.toEntity(): Comment =
        commentRepository
            .findBy(commentId)
            ?.takeIf { it.pageId == pageId }
            ?.takeIf { viewer.isAdmin || it.authorId == viewer.userId }
            ?: throw NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND)

    private fun Comment.editWith(request: Request): Comment =
        apply {
            edit(body = request.body)
        }

    private fun Comment.toResult(): Result =
        Result(
            commentId = id,
            authorHandle = userHandleQuery.handlesOf(setOf(authorId))[authorId]?.value ?: "",
            body = body,
            updatedAt = updatedAt
        )
}

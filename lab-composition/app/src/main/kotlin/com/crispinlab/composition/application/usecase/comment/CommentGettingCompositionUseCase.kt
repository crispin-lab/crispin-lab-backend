package com.crispinlab.composition.application.usecase.comment

import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.comment.CommentGettingComposition
import com.crispinlab.composition.application.port.incoming.comment.CommentGettingComposition.Request
import com.crispinlab.composition.application.port.incoming.comment.CommentGettingComposition.Result
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.composition.application.port.outgoing.user.handleOf
import com.crispinlab.space.application.port.incoming.comment.CommentGetting
import org.springframework.stereotype.Service

@Service
class CommentGettingCompositionUseCase(
    private val commentGetting: CommentGetting,
    private val userHandleLookup: UserHandleLookup,
    private val transactionProvider: TransactionProvider
) : CommentGettingComposition {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .toDomainRequest()
                .let { commentGetting.perform(it) }
                .toResult()
        }

    private fun Request.toDomainRequest(): CommentGetting.Request =
        CommentGetting.Request(
            pageId = pageId,
            commentId = commentId,
            viewer = viewer
        )

    private fun CommentGetting.Result.toResult(): Result =
        Result(
            commentId = commentId,
            pageId = pageId,
            authorId = authorId,
            authorHandle = userHandleLookup.handleOf(authorId),
            content = content,
            canEdit = canEdit,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

package com.crispinlab.composition.application.usecase.comment

import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.comment.CommentEditingComposition
import com.crispinlab.composition.application.port.incoming.comment.CommentEditingComposition.Request
import com.crispinlab.composition.application.port.incoming.comment.CommentEditingComposition.Result
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.composition.application.port.outgoing.user.handleOf
import com.crispinlab.space.application.port.incoming.comment.CommentEditing
import org.springframework.stereotype.Service

@Service
class CommentEditingCompositionUseCase(
    private val commentEditing: CommentEditing,
    private val userHandleLookup: UserHandleLookup,
    private val transactionProvider: TransactionProvider
) : CommentEditingComposition {
    override fun perform(request: Request): Result =
        request
            .toDomainRequest()
            .let { commentEditing.perform(it) }
            .toResult()

    private fun Request.toDomainRequest(): CommentEditing.Request =
        CommentEditing.Request(
            pageId = pageId,
            commentId = commentId,
            content = content,
            viewer = viewer
        )

    private fun CommentEditing.Result.toResult(): Result =
        Result(
            commentId = commentId,
            authorHandle =
                runCatching {
                    transactionProvider.transactional(readOnly = true) {
                        userHandleLookup.handleOf(authorId)
                    }
                }.getOrElse { "" },
            content = content,
            updatedAt = updatedAt
        )
}

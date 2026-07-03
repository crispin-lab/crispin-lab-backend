package com.crispinlab.composition.application.usecase.comment

import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.comment.CommentRegisteringComposition
import com.crispinlab.composition.application.port.incoming.comment.CommentRegisteringComposition.Request
import com.crispinlab.composition.application.port.incoming.comment.CommentRegisteringComposition.Result
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.composition.application.port.outgoing.user.handleOf
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering
import org.springframework.stereotype.Service

@Service
class CommentRegisteringCompositionUseCase(
    private val commentRegistering: CommentRegistering,
    private val userHandleLookup: UserHandleLookup,
    private val transactionProvider: TransactionProvider
) : CommentRegisteringComposition {
    override fun perform(request: Request): Result =
        request
            .toDomainRequest()
            .let { commentRegistering.perform(it) }
            .toResult()

    private fun Request.toDomainRequest(): CommentRegistering.Request =
        CommentRegistering.Request(
            pageId = pageId,
            content = content,
            viewer = viewer
        )

    private fun CommentRegistering.Result.toResult(): Result =
        Result(
            commentId = commentId,
            authorHandle =
                runCatching {
                    transactionProvider.transactional(readOnly = true) {
                        userHandleLookup.handleOf(authorId)
                    }
                }.getOrElse { "" }
        )
}

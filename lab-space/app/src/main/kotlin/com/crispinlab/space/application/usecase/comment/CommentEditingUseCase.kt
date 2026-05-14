package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentEditing
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Request
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Result
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentErrorCode
import org.springframework.stereotype.Service

@Service
class CommentEditingUseCase(
    private val commentRepository: CommentRepository,
    private val transactionProvider: TransactionProvider
) : CommentEditing {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .also {
                    it.validate()
                }.toEntity()
                .editWith(request)
                .let {
                    commentRepository.save(it)
                }.toResult()
        }

    private fun Request.validate() {
        /*
        todo    :: 비공개 페이지·권한 모델 도입 시 외부 의존 검증을 둘 자리.
         author :: heechoel shin
         date   :: 2026-05-14T00:00:00KST
         ticket :: LAB-23
         */
    }

    private fun Request.toEntity(): Comment =
        commentRepository
            .findBy(commentId)
            ?.takeIf {
                it.pageId == pageId &&
                    it.authorId == currentUserId &&
                    !it.isDeleted
            } ?: throw NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND)

    private fun Comment.editWith(request: Request): Comment =
        apply {
            edit(body = request.body)
        }

    private fun Comment.toResult(): Result =
        Result(
            commentId = id.value.toString(),
            body = body,
            updatedAt = updatedAt
        )
}

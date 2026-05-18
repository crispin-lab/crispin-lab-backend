package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentGetting
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Request
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Result
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentErrorCode
import org.springframework.stereotype.Service

@Service
class CommentGettingUseCase(
    private val commentRepository: CommentRepository,
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
        /*
        todo    :: 비공개 페이지·권한 모델 도입 시 외부 의존 검증을 둘 자리. 현재는 누구나 조회 가능.
         author :: heechoel shin
         date   :: 2026-05-14T00:00:00KST
         ticket :: LAB-23
         */
    }

    private fun Request.toEntity(): Comment =
        commentRepository
            .findBy(commentId)
            ?.takeIf {
                it.pageId == pageId
            } ?: throw NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND)

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

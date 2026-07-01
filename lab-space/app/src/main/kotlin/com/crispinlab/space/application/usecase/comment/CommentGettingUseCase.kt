package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentGetting
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Request
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Result
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.canEdit
import com.crispinlab.space.application.usecase.access.requireReadablePage
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentErrorCode
import org.springframework.stereotype.Service

@Service
class CommentGettingUseCase(
    private val commentRepository: CommentRepository,
    private val pageRepository: PageRepository,
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : CommentGetting {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            val page =
                requireReadablePage(
                    pageRepository = pageRepository,
                    spaceRepository = spaceRepository,
                    spaceMemberRepository = spaceMemberRepository,
                    viewer = request.viewer,
                    pageId = request.pageId
                )
            request
                .toEntity()
                .let { comment ->
                    comment.toResult(
                        canEdit =
                            spaceMemberRepository.canEdit(
                                viewer = request.viewer,
                                authorId = comment.authorId,
                                spaceId = page.spaceId
                            )
                    )
                }
        }

    private fun Request.toEntity(): Comment =
        commentRepository
            .findBy(commentId)
            ?.takeIf { it.pageId == pageId }
            ?: throw NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND)

    private fun Comment.toResult(canEdit: Boolean): Result =
        Result(
            commentId = id,
            pageId = pageId,
            authorId = authorId,
            content = content.raw,
            canEdit = canEdit,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

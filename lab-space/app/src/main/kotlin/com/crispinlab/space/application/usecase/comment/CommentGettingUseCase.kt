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
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentErrorCode
import com.crispinlab.space.domain.page.Page
import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import org.springframework.stereotype.Service

@Service
class CommentGettingUseCase(
    private val commentRepository: CommentRepository,
    private val pageRepository: PageRepository,
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val userHandleQuery: UserHandleQuery,
    private val transactionProvider: TransactionProvider
) : CommentGetting {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .requirePage()
                .let { page ->
                    request
                        .toEntity()
                        .toResult(viewer = request.viewer, page = page)
                }
        }

    private fun Request.requirePage(): Page =
        requireReadablePage(pageRepository, spaceRepository, spaceMemberRepository, viewer, pageId)

    private fun Request.toEntity(): Comment =
        commentRepository
            .findBy(commentId)
            ?.takeIf { it.pageId == pageId }
            ?: throw NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND)

    private fun Comment.toResult(
        viewer: Viewer.Member,
        page: Page
    ): Result =
        Result(
            commentId = id,
            pageId = pageId,
            authorId = authorId,
            authorHandle = userHandleQuery.handlesOf(setOf(authorId))[authorId]?.value ?: "",
            body = body,
            canEdit =
                spaceMemberRepository.canEdit(
                    viewer = viewer,
                    authorId = authorId,
                    spaceId = page.spaceId
                ),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

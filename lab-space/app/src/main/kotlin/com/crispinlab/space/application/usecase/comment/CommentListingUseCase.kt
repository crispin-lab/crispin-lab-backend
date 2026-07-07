package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentListing
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Request
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Summary
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.canEditCommentOf
import com.crispinlab.space.application.usecase.access.requireReadablePage
import com.crispinlab.space.domain.comment.Comment
import org.springframework.stereotype.Service

@Service
class CommentListingUseCase(
    private val commentRepository: CommentRepository,
    private val pageRepository: PageRepository,
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : CommentListing {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request
                .also {
                    requireReadablePage(
                        pageRepository = pageRepository,
                        spaceRepository = spaceRepository,
                        spaceMemberRepository = spaceMemberRepository,
                        viewer = it.viewer,
                        pageId = it.pageId
                    )
                }.toResult()
        }

    private fun Request.toResult(): PageResult<Summary> =
        commentRepository
            .findByPageId(pageId, pageRequest)
            .map { comment ->
                comment.toSummary(canEdit = viewer.canEditCommentOf(comment.authorId))
            }

    private fun Comment.toSummary(canEdit: Boolean): Summary =
        Summary(
            commentId = id,
            pageId = pageId,
            authorId = authorId,
            content = content.raw,
            canEdit = canEdit,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

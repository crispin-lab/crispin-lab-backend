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
import com.crispinlab.space.application.usecase.access.canEdit
import com.crispinlab.space.application.usecase.access.requireReadablePage
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.page.Page
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
            val page =
                requireReadablePage(
                    pageRepository = pageRepository,
                    spaceRepository = spaceRepository,
                    spaceMemberRepository = spaceMemberRepository,
                    viewer = request.viewer,
                    pageId = request.pageId
                )
            request.toResult(page = page)
        }

    private fun Request.toResult(page: Page): PageResult<Summary> =
        commentRepository
            .findByPageId(pageId, pageRequest)
            .let { comments ->
                val authorIds = comments.items.map { it.authorId }
                val canEditByAuthor =
                    spaceMemberRepository.canEdit(
                        viewer = viewer,
                        authorIds = authorIds,
                        spaceId = page.spaceId
                    )
                comments.map { comment ->
                    comment.toSummary(
                        canEdit = canEditByAuthor[comment.authorId] == true
                    )
                }
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

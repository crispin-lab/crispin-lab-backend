package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentEditing
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Request
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Result
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.usecase.access.handleOrEmpty
import com.crispinlab.space.application.usecase.mention.MentionDispatcher
import com.crispinlab.space.application.usecase.mention.extractMentions
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentErrorCode
import com.crispinlab.space.domain.mention.Mention
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.space.SpaceErrorCode
import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class CommentEditingUseCase(
    private val commentRepository: CommentRepository,
    private val pageRepository: PageRepository,
    private val spaceRepository: SpaceRepository,
    private val userHandleQuery: UserHandleQuery,
    private val mentionDispatcher: MentionDispatcher,
    private val transactionProvider: TransactionProvider,
    private val objectMapper: ObjectMapper
) : CommentEditing {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .toEntity()
                .editWith(request)
                .toResult()
        }

    private fun Request.toEntity(): Comment =
        commentRepository
            .findBy(commentId)
            ?.takeIf { it.pageId == pageId }
            ?.takeIf { viewer.isAdmin || it.authorId == viewer.userId }
            ?: throw NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND)

    private fun Comment.editWith(request: Request): Comment =
        apply {
            if (content.raw != request.content.raw) {
                edit(content = request.content)
                commentRepository.save(this)
                dispatchMentions()
            }
        }

    private fun Comment.dispatchMentions() {
        val page: Page =
            pageRepository.findBy(pageId)
                ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
        val spaceVisibility =
            spaceRepository.findVisibility(page.spaceId)
                ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)
        mentionDispatcher.dispatch(
            sourceType = Mention.SourceType.COMMENT,
            sourceId = id.value,
            actorUserId = authorId,
            extracted = content.extractMentions(objectMapper),
            subject =
                MentionDispatcher.MentionSubject(
                    spaceId = page.spaceId,
                    spaceVisibility = spaceVisibility,
                    pageVisibility = page.visibility,
                    pageAuthorId = page.authorId
                ),
            occurredAt = updatedAt
        )
    }

    private fun Comment.toResult(): Result =
        Result(
            commentId = id,
            authorHandle = userHandleQuery.handleOrEmpty(authorId),
            content = content.raw,
            updatedAt = updatedAt
        )
}

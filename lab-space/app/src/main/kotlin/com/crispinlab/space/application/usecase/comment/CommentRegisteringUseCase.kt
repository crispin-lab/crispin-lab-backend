package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Request
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Result
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.requireCommentPermission
import com.crispinlab.space.application.usecase.access.requireReadablePage
import com.crispinlab.space.application.usecase.mention.MentionDispatcher
import com.crispinlab.space.application.usecase.mention.extractMentions
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.mention.Mention
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.space.SpaceErrorCode
import com.crispinlab.space.domain.space.SpaceVisibility
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class CommentRegisteringUseCase(
    private val commentRepository: CommentRepository,
    private val pageRepository: PageRepository,
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val mentionDispatcher: MentionDispatcher,
    private val idGenerator: IdGenerator,
    private val transactionProvider: TransactionProvider,
    private val objectMapper: ObjectMapper
) : CommentRegistering {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            val page = request.validateAndResolvePage()
            val spaceVisibility = page.spaceVisibility()
            request
                .toEntity()
                .let { commentRepository.save(it) }
                .also { it.dispatchMentions(page, spaceVisibility) }
                .toResult()
        }

    private fun Request.validateAndResolvePage(): Page {
        val page =
            requireReadablePage(
                pageRepository,
                spaceRepository,
                spaceMemberRepository,
                viewer,
                pageId
            )
        viewer.requireCommentPermission()
        return page
    }

    private fun Page.spaceVisibility(): SpaceVisibility =
        spaceRepository.findVisibility(spaceId)
            ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)

    private fun Request.toEntity(): Comment =
        Comment(
            id = CommentId(idGenerator.next()),
            pageId = pageId,
            authorId = viewer.userId,
            content = content
        )

    private fun Comment.dispatchMentions(
        page: Page,
        spaceVisibility: SpaceVisibility
    ) {
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
            occurredAt = createdAt
        )
    }

    private fun Comment.toResult(): Result =
        Result(
            commentId = id,
            authorId = authorId
        )
}

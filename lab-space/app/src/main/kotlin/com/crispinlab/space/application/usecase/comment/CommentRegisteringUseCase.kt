package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Request
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Result
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.requireReadablePage
import com.crispinlab.space.application.usecase.access.requireWritePermission
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import org.springframework.stereotype.Service

@Service
class CommentRegisteringUseCase(
    private val commentRepository: CommentRepository,
    private val pageRepository: PageRepository,
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val userHandleQuery: UserHandleQuery,
    private val idGenerator: IdGenerator,
    private val transactionProvider: TransactionProvider
) : CommentRegistering {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .also {
                    it.validate()
                }.toEntity()
                .let {
                    commentRepository.save(it)
                }.toResult()
        }

    private fun Request.validate() {
        val page =
            requireReadablePage(
                pageRepository,
                spaceRepository,
                spaceMemberRepository,
                viewer,
                pageId
            )
        spaceMemberRepository.requireWritePermission(viewer, page.spaceId)
    }

    private fun Request.toEntity(): Comment =
        Comment(
            id = CommentId(idGenerator.next()),
            pageId = pageId,
            authorId = viewer.userId,
            body = body
        )

    private fun Comment.toResult(): Result =
        Result(
            commentId = id,
            authorHandle = userHandleQuery.handlesOf(setOf(authorId))[authorId]?.value ?: ""
        )
}

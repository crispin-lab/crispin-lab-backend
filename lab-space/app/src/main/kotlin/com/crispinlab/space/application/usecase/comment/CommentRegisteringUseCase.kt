package com.crispinlab.space.application.usecase.comment

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Request
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Result
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.lookupMemberSpaceIds
import com.crispinlab.space.application.usecase.access.requireWritePermission
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageErrorCode
import org.springframework.stereotype.Service

@Service
class CommentRegisteringUseCase(
    private val commentRepository: CommentRepository,
    private val pageRepository: PageRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
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
        val scope =
            VisibilityScope.of(viewer, spaceMemberRepository.lookupMemberSpaceIds(viewer))
        val page: Page =
            pageRepository
                .findBy(pageId)
                ?.takeIf { scope.allows(it.visibility, it.spaceId, it.authorId) }
                ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
        spaceMemberRepository.requireWritePermission(viewer, page.spaceId)
    }

    private fun Request.toEntity(): Comment =
        Comment(
            id = CommentId(idGenerator.next()),
            pageId = pageId,
            authorId = viewer.userId,
            body = body
        )

    private fun Comment.toResult(): Result = Result(commentId = id)
}

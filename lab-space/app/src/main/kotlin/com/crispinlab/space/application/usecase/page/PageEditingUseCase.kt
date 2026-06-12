package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageEditing
import com.crispinlab.space.application.port.incoming.page.PageEditing.Request
import com.crispinlab.space.application.port.incoming.page.PageEditing.Result
import com.crispinlab.space.application.port.outgoing.page.PageLinkRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageRevisionRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.requireWritePermission
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.page.PageLink
import com.crispinlab.space.domain.page.PageLinkId
import com.crispinlab.space.domain.page.PageRevision
import com.crispinlab.space.domain.page.PageRevisionId
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class PageEditingUseCase(
    private val pageRepository: PageRepository,
    private val pageRevisionRepository: PageRevisionRepository,
    private val pageLinkRepository: PageLinkRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val idGenerator: IdGenerator,
    private val transactionProvider: TransactionProvider,
    private val objectMapper: ObjectMapper
) : PageEditing {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .toEntity()
                .applyEditWith(request)
                .toResult()
        }

    private fun Request.toEntity(): Page =
        pageRepository
            .findBy(pageId)
            ?.takeIf {
                viewer.isAdmin || it.authorId == viewer.userId
            }?.also {
                spaceMemberRepository.requireWritePermission(viewer, it.spaceId)
            } ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)

    private fun Page.applyEditWith(request: Request): Page =
        apply {
            val visibilityChanged: Boolean =
                request.visibility
                    ?.takeIf { it != visibility }
                    ?.also { changeVisibility(it) } != null
            val editResult: Page.EditResult? =
                takeIf { it.needsNewRevision(request) }
                    ?.edit(
                        title = request.title,
                        content = request.content
                    )
            if (visibilityChanged || editResult != null) {
                pageRepository.save(this)
            }
            editResult?.let { saveRevisionAndLinksWith(it) }
        }

    private fun Page.needsNewRevision(request: Request): Boolean =
        title != request.title || content.raw != request.content

    private fun Page.saveRevisionAndLinksWith(editResult: Page.EditResult) {
        val revision: PageRevision = saveRevisionWith(editResult)
        saveLinksWith(editResult, revisionId = revision.id)
    }

    private fun Page.saveRevisionWith(editResult: Page.EditResult): PageRevision =
        PageRevision(
            id = PageRevisionId(idGenerator.next()),
            pageId = id,
            version = editResult.version,
            title = editResult.title,
            content = editResult.content,
            authorId = authorId,
            createdAt = editResult.occurredAt
        ).let {
            pageRevisionRepository.save(it)
        }

    private fun Page.saveLinksWith(
        editResult: Page.EditResult,
        revisionId: PageRevisionId
    ) {
        editResult.content
            .extractPageLinks(objectMapper)
            .map { extracted ->
                PageLink(
                    id = PageLinkId(idGenerator.next()),
                    pageId = id,
                    revisionId = revisionId,
                    target = extracted.targetPageId,
                    createdAt = editResult.occurredAt
                )
            }.let {
                pageLinkRepository.saveAll(it)
            }
    }

    private fun Page.toResult(): Result =
        Result(
            pageId = id,
            title = title,
            version = currentVersion,
            updatedAt = updatedAt
        )
}

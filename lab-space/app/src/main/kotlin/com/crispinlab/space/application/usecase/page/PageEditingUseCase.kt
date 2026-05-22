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
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.page.PageLink
import com.crispinlab.space.domain.page.PageLinkId
import com.crispinlab.space.domain.page.PageRevision
import com.crispinlab.space.domain.page.PageRevisionId
import org.springframework.stereotype.Service

@Service
class PageEditingUseCase(
    private val pageRepository: PageRepository,
    private val pageRevisionRepository: PageRevisionRepository,
    private val pageLinkRepository: PageLinkRepository,
    private val idGenerator: IdGenerator,
    private val transactionProvider: TransactionProvider
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
                auth.isAdmin || it.authorId == auth.userId
            } ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)

    private fun Page.applyEditWith(request: Request): Page =
        apply {
            val editResult: Page.EditResult =
                edit(
                    title = request.title,
                    content = request.content
                )
            pageRepository.save(this)
            saveRevisionAndLinksWith(editResult)
        }

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
        editResult.wikiLinks
            .map { extracted ->
                PageLink(
                    id = PageLinkId(idGenerator.next()),
                    pageId = id,
                    revisionId = revisionId,
                    target = extracted.target,
                    type = extracted.type,
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

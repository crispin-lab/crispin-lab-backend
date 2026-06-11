package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageRegistering
import com.crispinlab.space.application.port.incoming.page.PageRegistering.Request
import com.crispinlab.space.application.port.incoming.page.PageRegistering.Result
import com.crispinlab.space.application.port.outgoing.page.PageLinkRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageRevisionRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.requireWritePermission
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageLink
import com.crispinlab.space.domain.page.PageLinkId
import com.crispinlab.space.domain.page.PageRevision
import com.crispinlab.space.domain.page.PageRevisionId
import com.crispinlab.space.domain.space.SpaceErrorCode
import org.springframework.stereotype.Service

@Service
class PageRegisteringUseCase(
    private val pageRepository: PageRepository,
    private val pageRevisionRepository: PageRevisionRepository,
    private val pageLinkRepository: PageLinkRepository,
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val idGenerator: IdGenerator,
    private val transactionProvider: TransactionProvider
) : PageRegistering {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .also {
                    it.validate()
                }.toEntity()
                .let {
                    pageRepository.save(it)
                }.saveInitialRevisionAndLinks()
                .toResult()
        }

    private fun Request.validate() {
        spaceRepository.findBy(spaceId)
            ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)
        spaceMemberRepository.requireWritePermission(viewer, spaceId)
        parentPageId?.let { parentId ->
            pageRepository
                .findBy(parentId)
                ?.takeIf {
                    it.spaceId == spaceId
                } ?: throw NotFoundException(PageErrorCode.PARENT_PAGE_NOT_FOUND)
        }
    }

    private fun Request.toEntity(): Page =
        Page(
            id = PageId(idGenerator.next()),
            spaceId = spaceId,
            parentPageId = parentPageId,
            authorId = viewer.userId,
            title = title,
            content = PageContent(content),
            visibility = visibility,
            currentVersion = 1,
            displayOrder = pageRepository.nextDisplayOrderIn(spaceId, parentPageId)
        )

    private fun Page.saveInitialRevisionAndLinks(): Page =
        apply {
            val revision: PageRevision = saveInitialRevision()
            saveInitialLinksWith(revisionId = revision.id)
        }

    private fun Page.saveInitialRevision(): PageRevision =
        PageRevision(
            id = PageRevisionId(idGenerator.next()),
            pageId = id,
            version = currentVersion,
            title = title,
            content = content,
            authorId = authorId,
            createdAt = createdAt
        ).let {
            pageRevisionRepository.save(it)
        }

    private fun Page.saveInitialLinksWith(revisionId: PageRevisionId) {
        content
            .extractLinks()
            .map { extracted ->
                PageLink(
                    id = PageLinkId(idGenerator.next()),
                    pageId = id,
                    revisionId = revisionId,
                    target = extracted.toTarget(),
                    createdAt = createdAt
                )
            }.let {
                pageLinkRepository.saveAll(it)
            }
    }

    private fun Page.toResult(): Result = Result(pageId = id)
}

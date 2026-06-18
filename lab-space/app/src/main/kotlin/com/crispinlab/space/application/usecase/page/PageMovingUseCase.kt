package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageMoving
import com.crispinlab.space.application.port.incoming.page.PageMoving.Request
import com.crispinlab.space.application.port.outgoing.page.PageAncestorPort
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.requireWritePermission
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.page.PageId
import org.springframework.stereotype.Service

@Service
class PageMovingUseCase(
    private val pageRepository: PageRepository,
    private val pageAncestorPort: PageAncestorPort,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : PageMoving {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            request
                .toEntity()
                .moveWith(request)
                .let {
                    pageRepository.save(it)
                }
        }
    }

    private fun Request.toEntity(): Page =
        pageRepository
            .findBy(pageId)
            ?.takeIf {
                viewer.isAdmin || it.authorId == viewer.userId
            }?.also {
                spaceMemberRepository.requireWritePermission(viewer, it.spaceId)
            } ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)

    private fun Page.moveWith(request: Request): Page =
        apply {
            requireSameSpaceParent(request.parentPageId)
            requireNoCycle(request.parentPageId)
            requireParentChanged(request.parentPageId)
            move(
                parentPageId = request.parentPageId,
                displayOrder = pageRepository.nextDisplayOrderIn(spaceId, request.parentPageId)
            )
        }

    private fun Page.requireSameSpaceParent(newParentPageId: PageId?) {
        newParentPageId?.let { parentId ->
            pageRepository
                .findBy(parentId)
                ?.takeIf {
                    it.spaceId == spaceId
                } ?: throw NotFoundException(PageErrorCode.PARENT_PAGE_NOT_FOUND)
        }
    }

    private fun Page.requireNoCycle(newParentPageId: PageId?) {
        if (newParentPageId == null) return
        val cycle =
            newParentPageId == id ||
                pageAncestorPort
                    .findAncestorsOf(newParentPageId)
                    .any { it.pageId == id }
        if (cycle) throw ConflictException(PageErrorCode.PAGE_PARENT_CYCLE)
    }

    private fun Page.requireParentChanged(newParentPageId: PageId?) {
        if (newParentPageId == parentPageId) {
            throw ConflictException(PageErrorCode.PAGE_PARENT_UNCHANGED)
        }
    }
}

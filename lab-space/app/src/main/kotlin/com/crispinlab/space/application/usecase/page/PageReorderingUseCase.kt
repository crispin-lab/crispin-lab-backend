package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageReordering
import com.crispinlab.space.application.port.incoming.page.PageReordering.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.requireWritePermission
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageErrorCode
import org.springframework.stereotype.Service

@Service
class PageReorderingUseCase(
    private val pageRepository: PageRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : PageReordering {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            request
                .toEntity()
                .reorderWith(request)
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

    private fun Page.reorderWith(request: Request): Page =
        apply {
            reorder(request.displayOrder)
        }
}

package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageDeleting
import com.crispinlab.space.application.port.incoming.page.PageDeleting.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.requireWritePermission
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageErrorCode
import org.springframework.stereotype.Service

@Service
class PageDeletingUseCase(
    private val pageRepository: PageRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : PageDeleting {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            request
                .toEntity()
                .withdraw()
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

    private fun Page.withdraw() {
        pageRepository.delete(id)
    }
}

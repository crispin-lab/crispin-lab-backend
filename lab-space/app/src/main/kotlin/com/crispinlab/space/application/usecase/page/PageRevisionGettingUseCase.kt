package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageRevisionGetting
import com.crispinlab.space.application.port.incoming.page.PageRevisionGetting.Request
import com.crispinlab.space.application.port.incoming.page.PageRevisionGetting.Result
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageRevisionRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.findReadablePage
import com.crispinlab.space.domain.page.PageRevision
import com.crispinlab.space.domain.page.PageRevisionErrorCode
import org.springframework.stereotype.Service

@Service
class PageRevisionGettingUseCase(
    private val pageRepository: PageRepository,
    private val pageRevisionRepository: PageRevisionRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : PageRevisionGetting {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .also {
                    it.validate()
                }.toEntity()
                .toResult()
        }

    private fun Request.validate() {
        findReadablePage(pageRepository, spaceMemberRepository, viewer, pageId)
    }

    private fun Request.toEntity(): PageRevision =
        pageRevisionRepository
            .findBy(pageId, version)
            ?: throw NotFoundException(PageRevisionErrorCode.PAGE_REVISION_NOT_FOUND)

    private fun PageRevision.toResult(): Result =
        Result(
            revisionId = id,
            pageId = pageId,
            version = version,
            title = title,
            content = content.raw,
            authorId = authorId,
            createdAt = createdAt
        )
}

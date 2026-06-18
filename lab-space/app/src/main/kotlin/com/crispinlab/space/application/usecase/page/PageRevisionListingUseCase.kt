package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageRevisionListing
import com.crispinlab.space.application.port.incoming.page.PageRevisionListing.Request
import com.crispinlab.space.application.port.incoming.page.PageRevisionListing.Summary
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageRevisionRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.requireReadablePage
import com.crispinlab.space.domain.page.PageRevision
import org.springframework.stereotype.Service

@Service
class PageRevisionListingUseCase(
    private val pageRepository: PageRepository,
    private val pageRevisionRepository: PageRevisionRepository,
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : PageRevisionListing {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request
                .also {
                    it.validate()
                }.toResult()
        }

    private fun Request.validate() {
        requireReadablePage(pageRepository, spaceRepository, spaceMemberRepository, viewer, pageId)
    }

    private fun Request.toResult(): PageResult<Summary> =
        pageRevisionRepository
            .findByPageId(pageId, pageRequest)
            .map { it.toSummary() }

    private fun PageRevision.toSummary(): Summary =
        Summary(
            revisionId = id,
            pageId = pageId,
            version = version,
            title = title,
            authorId = authorId,
            createdAt = createdAt
        )
}

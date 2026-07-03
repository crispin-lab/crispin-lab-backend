package com.crispinlab.composition.application.usecase.page

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.page.PageInboundLinkListingComposition
import com.crispinlab.composition.application.port.incoming.page.PageInboundLinkListingComposition.Request
import com.crispinlab.composition.application.port.incoming.page.PageInboundLinkListingComposition.Result
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing.Summary
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Service

@Service
class PageInboundLinkListingCompositionUseCase(
    private val pageInboundLinkListing: PageInboundLinkListing,
    private val userHandleLookup: UserHandleLookup,
    private val transactionProvider: TransactionProvider
) : PageInboundLinkListingComposition {
    override fun perform(request: Request): PageResult<Result> =
        transactionProvider.transactional(readOnly = true) {
            request
                .toDomainRequest()
                .let { pageInboundLinkListing.perform(it) }
                .toResults()
        }

    private fun Request.toDomainRequest(): PageInboundLinkListing.Request =
        PageInboundLinkListing.Request(
            pageId = pageId,
            page = page,
            size = size,
            viewer = viewer
        )

    private fun PageResult<Summary>.toResults(): PageResult<Result> {
        val handles = userHandleLookup.handlesOf(items.map { it.authorId }.toSet())
        return map { it.toResult(handles) }
    }

    private fun Summary.toResult(handles: Map<UserId, String>): Result =
        Result(
            pageId = pageId,
            spaceId = spaceId,
            parentPageId = parentPageId,
            authorId = authorId,
            authorHandle = handles[authorId] ?: "",
            title = title,
            visibility = visibility,
            displayOrder = displayOrder,
            updatedAt = updatedAt
        )
}

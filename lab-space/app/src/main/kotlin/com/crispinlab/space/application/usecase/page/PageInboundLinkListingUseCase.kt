package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing.Request
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing.Summary
import com.crispinlab.space.application.port.outgoing.page.PageInboundLinkPort
import com.crispinlab.space.application.port.outgoing.page.PageInboundLinkPort.InboundLinkSummary
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.memberSpaceIdsOf
import com.crispinlab.space.application.usecase.access.requireReadablePage
import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Service

@Service
class PageInboundLinkListingUseCase(
    private val pageRepository: PageRepository,
    private val pageInboundLinkPort: PageInboundLinkPort,
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val userHandleQuery: UserHandleQuery,
    private val transactionProvider: TransactionProvider
) : PageInboundLinkListing {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request
                .toScope()
                .let { scope ->
                    requireReadablePage(pageRepository, spaceRepository, scope, request.pageId)
                    pageInboundLinkPort
                        .findInboundLinksOf(request.pageId, scope, request.pageRequest)
                }.toSummaries()
        }

    private fun Request.toScope(): VisibilityScope =
        VisibilityScope.of(viewer, spaceMemberRepository.memberSpaceIdsOf(viewer))

    private fun PageResult<InboundLinkSummary>.toSummaries(): PageResult<Summary> {
        val authorIds = items.map { it.authorId }.toSet()
        val handles =
            if (authorIds.isEmpty()) emptyMap() else userHandleQuery.handlesOf(authorIds)
        return map { it.toSummary(handles) }
    }

    private fun InboundLinkSummary.toSummary(handles: Map<UserId, Handle>): Summary =
        Summary(
            pageId = pageId,
            spaceId = spaceId,
            parentPageId = parentPageId,
            authorId = authorId,
            authorHandle = handles[authorId]?.value ?: "",
            title = title,
            visibility = visibility,
            displayOrder = displayOrder,
            updatedAt = updatedAt
        )
}

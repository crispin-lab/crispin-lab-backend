package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageSearching
import com.crispinlab.space.application.port.incoming.page.PageSearching.Request
import com.crispinlab.space.application.port.incoming.page.PageSearching.Summary
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.PageSummary
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.memberSpaceIdsOf
import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Service

@Service
class PageSearchingUseCase(
    private val pageSearchPort: PageSearchPort,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val userHandleQuery: UserHandleQuery,
    private val transactionProvider: TransactionProvider
) : PageSearching {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request
                .search()
                .toSummaries()
        }

    private fun Request.toScope(): VisibilityScope =
        VisibilityScope.of(viewer, spaceMemberRepository.memberSpaceIdsOf(viewer))

    private fun Request.search(): PageResult<PageSummary> =
        pageSearchPort.search(
            keyword = keyword,
            spaceId = spaceId,
            tagIds = tagIds,
            sort = sort,
            scope = toScope(),
            pageRequest = pageRequest
        )

    private fun PageResult<PageSummary>.toSummaries(): PageResult<Summary> {
        val authorIds = items.map { it.authorId }.toSet()
        val handles =
            if (authorIds.isEmpty()) emptyMap() else userHandleQuery.handlesOf(authorIds)
        return map { it.toSummary(handles) }
    }

    private fun PageSummary.toSummary(handles: Map<UserId, Handle>): Summary =
        Summary(
            pageId = id,
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

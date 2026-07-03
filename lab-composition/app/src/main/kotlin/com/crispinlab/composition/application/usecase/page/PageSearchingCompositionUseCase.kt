package com.crispinlab.composition.application.usecase.page

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.page.PageSearchingComposition
import com.crispinlab.composition.application.port.incoming.page.PageSearchingComposition.Request
import com.crispinlab.composition.application.port.incoming.page.PageSearchingComposition.Result
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.space.application.port.incoming.page.PageSearching
import com.crispinlab.space.application.port.incoming.page.PageSearching.Summary
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Service

@Service
class PageSearchingCompositionUseCase(
    private val pageSearching: PageSearching,
    private val userHandleLookup: UserHandleLookup,
    private val transactionProvider: TransactionProvider
) : PageSearchingComposition {
    override fun perform(request: Request): PageResult<Result> =
        transactionProvider.transactional(readOnly = true) {
            request
                .toDomainRequest()
                .let { pageSearching.perform(it) }
                .toResults()
        }

    private fun Request.toDomainRequest(): PageSearching.Request =
        PageSearching.Request(
            keyword = keyword,
            spaceId = spaceId,
            tagIds = tagIds,
            tagName = tagName,
            sort = sort,
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

package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageSearching
import com.crispinlab.space.application.port.incoming.page.PageSearching.Request
import com.crispinlab.space.application.port.incoming.page.PageSearching.Summary
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.PageSummary
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import org.springframework.stereotype.Service

@Service
class PageSearchingUseCase(
    private val pageSearchPort: PageSearchPort,
    private val transactionProvider: TransactionProvider
) : PageSearching {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request.toResult()
        }

    private fun Request.toResult(): PageResult<Summary> =
        pageSearchPort
            .search(
                keyword = keyword,
                spaceId = spaceId,
                tagIds = tagIds,
                scope = VisibilityScope.of(viewer),
                pageRequest = pageRequest
            ).map { it.toSummary() }

    private fun PageSummary.toSummary(): Summary =
        Summary(
            pageId = id,
            spaceId = spaceId,
            title = title,
            updatedAt = updatedAt
        )
}

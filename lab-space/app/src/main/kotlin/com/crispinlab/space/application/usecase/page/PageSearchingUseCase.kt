package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageSearching
import com.crispinlab.space.application.port.incoming.page.PageSearching.Request
import com.crispinlab.space.application.port.incoming.page.PageSearching.Summary
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.PageSummary
import org.springframework.stereotype.Service

@Service
class PageSearchingUseCase(
    private val pageSearchPort: PageSearchPort,
    private val transactionProvider: TransactionProvider
) : PageSearching {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request
                .also { it.validate() }
                .toResult()
        }

    private fun Request.validate() {
        /*
        todo    :: 권한·스페이스 가시성 등 외부 의존 검증을 둘 자리. 인증 도입 이후 채운다.
         author :: heechoel shin
         date   :: 2026-05-15T17:00:00KST
         ticket :: LAB-25
         */
    }

    private fun Request.toResult(): PageResult<Summary> =
        pageSearchPort
            .search(
                keyword = keyword,
                spaceId = spaceId,
                tagIds = tagIds,
                pageRequest = pageRequest
            ).map { it.toSummary() }

    private fun PageSummary.toSummary(): Summary =
        Summary(
            pageId = id.value.toString(),
            spaceId = spaceId.value.toString(),
            title = title,
            updatedAt = updatedAt
        )
}

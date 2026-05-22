package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageSearching
import com.crispinlab.space.application.port.incoming.page.PageSearching.Request
import com.crispinlab.space.application.port.incoming.page.PageSearching.Summary
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.PageSummary
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.UserId
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
                visibilities = allowedVisibilities(),
                draftAuthorId = draftAuthorId(),
                pageRequest = pageRequest
            ).map { it.toSummary() }

    private fun Request.allowedVisibilities(): Set<Visibility> =
        when {
            currentUserRole == SystemRole.ADMIN -> {
                setOf(
                    Visibility.PUBLIC,
                    Visibility.INTERNAL,
                    Visibility.DRAFT
                )
            }

            currentUserId != null -> {
                setOf(Visibility.PUBLIC, Visibility.INTERNAL)
            }

            else -> {
                setOf(Visibility.PUBLIC)
            }
        }

    private fun Request.draftAuthorId(): UserId? =
        currentUserId?.takeIf { currentUserRole != SystemRole.ADMIN }

    private fun PageSummary.toSummary(): Summary =
        Summary(
            pageId = id,
            spaceId = spaceId,
            title = title,
            updatedAt = updatedAt
        )
}

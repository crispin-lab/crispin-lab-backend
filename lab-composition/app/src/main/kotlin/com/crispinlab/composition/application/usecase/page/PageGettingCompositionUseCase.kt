package com.crispinlab.composition.application.usecase.page

import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.page.PageGettingComposition
import com.crispinlab.composition.application.port.incoming.page.PageGettingComposition.Request
import com.crispinlab.composition.application.port.incoming.page.PageGettingComposition.Result
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.composition.application.port.outgoing.user.handleOf
import com.crispinlab.space.application.port.incoming.page.PageGetting
import org.springframework.stereotype.Service

@Service
class PageGettingCompositionUseCase(
    private val pageGetting: PageGetting,
    private val userHandleLookup: UserHandleLookup,
    private val transactionProvider: TransactionProvider
) : PageGettingComposition {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .toDomainRequest()
                .let { pageGetting.perform(it) }
                .toResult()
        }

    private fun Request.toDomainRequest(): PageGetting.Request =
        PageGetting.Request(
            pageId = pageId,
            viewer = viewer
        )

    private fun PageGetting.Result.toResult(): Result =
        Result(
            pageId = pageId,
            spaceId = spaceId,
            parentPageId = parentPageId,
            authorId = authorId,
            authorHandle = userHandleLookup.handleOf(authorId),
            title = title,
            content = content,
            visibility = visibility,
            currentVersion = currentVersion,
            displayOrder = displayOrder,
            canEdit = canEdit,
            canComment = canComment,
            createdAt = createdAt,
            updatedAt = updatedAt,
            ancestors = ancestors.map { it.toSummary() }
        )

    private fun PageGetting.Result.AncestorSummary.toSummary(): Result.AncestorSummary =
        Result.AncestorSummary(
            pageId = pageId,
            title = title
        )
}

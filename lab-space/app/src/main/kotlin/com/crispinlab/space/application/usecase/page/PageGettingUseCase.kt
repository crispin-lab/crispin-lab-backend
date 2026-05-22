package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageGetting
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.incoming.page.PageGetting.Result
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.page.Visibility
import org.springframework.stereotype.Service

@Service
class PageGettingUseCase(
    private val pageRepository: PageRepository,
    private val transactionProvider: TransactionProvider
) : PageGetting {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .toEntity()
                .toResult()
        }

    private fun Request.toEntity(): Page =
        pageRepository
            .findBy(pageId)
            ?.takeIf { it.isVisibleFor(viewer) }
            ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)

    private fun Page.isVisibleFor(viewer: Viewer): Boolean =
        when (visibility) {
            Visibility.PUBLIC -> true
            Visibility.INTERNAL -> viewer.isAuthenticated
            Visibility.DRAFT -> viewer.isAdmin || isOwnedBy(viewer)
        }

    private fun Page.isOwnedBy(viewer: Viewer): Boolean =
        viewer is Viewer.Member && authorId == viewer.userId

    private fun Page.toResult(): Result =
        Result(
            pageId = id,
            spaceId = spaceId,
            parentPageId = parentPageId,
            authorId = authorId,
            title = title,
            content = content.raw,
            visibility = visibility.name,
            currentVersion = currentVersion,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

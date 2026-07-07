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
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.application.usecase.access.memberSpaceIdsOf
import com.crispinlab.space.domain.tag.TagId
import org.springframework.stereotype.Service

@Service
class PageSearchingUseCase(
    private val pageSearchPort: PageSearchPort,
    private val tagRepository: TagRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : PageSearching {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request
                .search()
                .map { it.toSummary() }
        }

    private fun Request.toScope(): VisibilityScope =
        VisibilityScope.of(viewer, spaceMemberRepository.memberSpaceIdsOf(viewer))

    private fun Request.search(): PageResult<PageSummary> =
        resolveTagIdsAnyOf()
            ?.let { tagIdsAnyOf ->
                pageSearchPort.search(
                    keyword = keyword,
                    spaceId = spaceId,
                    tagIds = tagIds,
                    tagIdsAnyOf = tagIdsAnyOf,
                    parentPageId = parentPageId,
                    onlyRoot = onlyRoot,
                    sort = sort,
                    scope = toScope(),
                    pageRequest = pageRequest
                )
            } ?: PageResult.empty(pageRequest)

    private fun Request.resolveTagIdsAnyOf(): List<TagId>? {
        val name = tagName ?: return emptyList()
        return tagRepository.findIdsByName(name).ifEmpty { null }
    }

    private fun PageSummary.toSummary(): Summary =
        Summary(
            pageId = id,
            spaceId = spaceId,
            parentPageId = parentPageId,
            authorId = authorId,
            title = title,
            visibility = visibility,
            displayOrder = displayOrder,
            updatedAt = updatedAt
        )
}

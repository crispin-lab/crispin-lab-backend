package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing.Request
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing.Summary
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.port.outgoing.tag.TagPopularitySearchPort
import com.crispinlab.space.application.port.outgoing.tag.TagPopularitySearchPort.TagPopularitySummary
import com.crispinlab.space.application.usecase.access.memberSpaceIdsOf
import org.springframework.stereotype.Service

@Service
class TagPopularityListingUseCase(
    private val tagPopularitySearchPort: TagPopularitySearchPort,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : TagPopularityListing {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request.toResult()
        }

    private fun Request.toScope(): VisibilityScope =
        VisibilityScope.of(viewer, spaceMemberRepository.memberSpaceIdsOf(viewer))

    private fun Request.toResult(): PageResult<Summary> =
        tagPopularitySearchPort
            .search(scope = toScope(), pageRequest = pageRequest)
            .map { it.toSummary() }

    private fun TagPopularitySummary.toSummary(): Summary =
        Summary(
            name = name,
            usageCount = usageCount
        )
}

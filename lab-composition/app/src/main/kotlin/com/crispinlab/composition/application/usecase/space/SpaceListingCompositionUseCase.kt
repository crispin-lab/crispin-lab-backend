package com.crispinlab.composition.application.usecase.space

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.space.SpaceListingComposition
import com.crispinlab.composition.application.port.incoming.space.SpaceListingComposition.LatestPage
import com.crispinlab.composition.application.port.incoming.space.SpaceListingComposition.Request
import com.crispinlab.composition.application.port.incoming.space.SpaceListingComposition.Result
import com.crispinlab.composition.application.port.outgoing.space.PageStatLookup
import com.crispinlab.composition.application.port.outgoing.space.SpaceMembershipLookup
import com.crispinlab.space.application.port.incoming.space.SpaceListing
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Summary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import org.springframework.stereotype.Service

@Service
class SpaceListingCompositionUseCase(
    private val spaceListing: SpaceListing,
    private val spaceMembershipLookup: SpaceMembershipLookup,
    private val pageStatLookup: PageStatLookup,
    private val transactionProvider: TransactionProvider
) : SpaceListingComposition {
    override fun perform(request: Request): PageResult<Result> =
        transactionProvider.transactional(readOnly = true) {
            val memberSpaceIds = spaceMembershipLookup.memberSpaceIdsOf(request.viewer)
            request
                .toDomainRequest()
                .let { spaceListing.perform(it) }
                .toResultsFor(request.viewer, memberSpaceIds)
        }

    private fun Request.toDomainRequest(): SpaceListing.Request =
        SpaceListing.Request(
            keyword = keyword,
            sort = sort?.name,
            direction = direction?.name,
            page = pageRequest.page,
            size = pageRequest.size,
            viewer = viewer
        )

    private fun PageResult<Summary>.toResultsFor(
        viewer: Viewer,
        memberSpaceIds: Set<SpaceId>
    ): PageResult<Result> {
        val spaceIds = items.map { it.spaceId }.toSet()
        val roles = viewer.rolesFor(spaceIds)
        val memberCounts =
            runCatching { spaceMembershipLookup.memberCountsOf(spaceIds) }
                .getOrElse { emptyMap() }
        val pageStats =
            runCatching { pageStatLookup.countsAndLatestOf(spaceIds, viewer, memberSpaceIds) }
                .getOrElse { emptyMap() }
        return map { it.toResult(roles, memberCounts, pageStats) }
    }

    private fun Viewer.rolesFor(spaceIds: Set<SpaceId>): Map<SpaceId, SpaceMemberRole> =
        when (this) {
            is Viewer.Member -> {
                runCatching { spaceMembershipLookup.rolesOf(userId, spaceIds) }
                    .getOrElse { emptyMap() }
            }

            Viewer.Anonymous -> {
                emptyMap()
            }
        }

    private fun Summary.toResult(
        roles: Map<SpaceId, SpaceMemberRole>,
        memberCounts: Map<SpaceId, Long>,
        pageStats: Map<SpaceId, PageStatLookup.PageStat>
    ): Result {
        val stat = pageStats[spaceId]
        val latest = stat?.latest?.toLatestPage()
        return Result(
            spaceId = spaceId,
            name = name,
            description = description,
            visibility = visibility,
            myRole = roles[spaceId],
            memberCount = memberCounts[spaceId] ?: 0L,
            pageCount = stat?.count ?: 0L,
            lastActivityAt = lastActivityAt,
            latestPage = latest,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun PageStatLookup.LatestPage.toLatestPage(): LatestPage =
        LatestPage(
            pageId = pageId,
            title = title,
            updatedAt = updatedAt
        )
}

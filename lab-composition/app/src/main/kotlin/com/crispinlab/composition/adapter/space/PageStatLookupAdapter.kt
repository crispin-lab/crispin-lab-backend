package com.crispinlab.composition.adapter.space

import com.crispinlab.composition.application.port.outgoing.space.PageStatLookup
import com.crispinlab.composition.application.port.outgoing.space.PageStatLookup.LatestPage
import com.crispinlab.composition.application.port.outgoing.space.PageStatLookup.PageStat
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import java.time.Instant
import org.springframework.stereotype.Component

@Component
class PageStatLookupAdapter(
    private val pageSearchPort: PageSearchPort
) : PageStatLookup {
    override fun countsAndLatestOf(
        spaceIds: Collection<SpaceId>,
        viewer: Viewer,
        memberSpaceIds: Set<SpaceId>
    ): Map<SpaceId, PageStat> {
        val idSet = spaceIds.toSet()
        if (idSet.isEmpty()) return emptyMap()
        val scope = PageSearchPort.VisibilityScope.of(viewer, memberSpaceIds)
        return pageSearchPort
            .statsBySpaceIds(idSet, scope)
            .mapValues { it.value.toStat() }
    }

    override fun updatedCountsSince(
        sinceOf: Map<SpaceId, Instant?>,
        viewer: Viewer,
        memberSpaceIds: Set<SpaceId>
    ): Map<SpaceId, Long> {
        if (sinceOf.isEmpty()) return emptyMap()
        val threshold = sinceOf.mapValues { it.value ?: Instant.EPOCH }
        val scope = PageSearchPort.VisibilityScope.of(viewer, memberSpaceIds)
        return pageSearchPort.updatedCountsSince(threshold, scope)
    }

    private fun PageSearchPort.PageStat.toStat(): PageStat =
        PageStat(
            count = count,
            latest = latest?.toLatestPage()
        )

    private fun PageSearchPort.LatestPage.toLatestPage(): LatestPage =
        LatestPage(
            pageId = pageId,
            title = title,
            updatedAt = updatedAt
        )
}

package com.crispinlab.composition.application.port.outgoing.space

import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import java.time.Instant

interface PageStatLookup {
    fun countsAndLatestOf(
        spaceIds: Collection<SpaceId>,
        viewer: Viewer,
        memberSpaceIds: Set<SpaceId>
    ): Map<SpaceId, PageStat>

    data class PageStat(
        val count: Long,
        val latest: LatestPage?
    )

    data class LatestPage(
        val pageId: PageId,
        val title: String,
        val updatedAt: Instant
    )
}

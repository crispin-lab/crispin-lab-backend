package com.crispinlab.composition.application.port.incoming.space

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.incoming.space.SpaceListingComposition.Request
import com.crispinlab.composition.application.port.incoming.space.SpaceListingComposition.Result
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository.SortDirection
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository.SortDirection.Companion.asSortDirection
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository.SortOption
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository.SortOption.Companion.asSortOption
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import java.time.Instant

interface SpaceListingComposition : UseCase<Request, PageResult<Result>> {
    class Request(
        val keyword: String? = null,
        sort: String? = null,
        direction: String? = null,
        page: Int = 0,
        size: Int = DEFAULT_SIZE,
        val viewer: Viewer
    ) {
        val sort: SortOption? = sort?.trim()?.takeIf { it.isNotEmpty() }?.asSortOption()
        val direction: SortDirection? =
            direction
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.asSortDirection()
        val pageRequest: PageRequest =
            PageRequest(
                page = page,
                size = size
            )
    }

    data class Result(
        val spaceId: SpaceId,
        val name: String,
        val description: String,
        val visibility: SpaceVisibility,
        val myRole: SpaceMemberRole?,
        val memberCount: Long,
        val pageCount: Long,
        val lastActivityAt: Instant,
        val latestPage: LatestPage?,
        val createdAt: Instant,
        val updatedAt: Instant
    )

    data class LatestPage(
        val pageId: PageId,
        val title: String,
        val updatedAt: Instant
    )
}

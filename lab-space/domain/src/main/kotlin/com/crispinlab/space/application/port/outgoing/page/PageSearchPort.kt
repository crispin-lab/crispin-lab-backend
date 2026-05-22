package com.crispinlab.space.application.port.outgoing.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface PageSearchPort {
    fun search(
        keyword: String?,
        spaceId: SpaceId?,
        tagIds: Collection<TagId>,
        scope: VisibilityScope,
        pageRequest: PageRequest
    ): PageResult<PageSummary>

    sealed interface VisibilityScope {
        data object Anonymous : VisibilityScope

        data class Authenticated(
            val viewerId: UserId
        ) : VisibilityScope

        data object Privileged : VisibilityScope
    }

    data class PageSummary(
        val id: PageId,
        val spaceId: SpaceId,
        val title: String,
        val updatedAt: Instant
    )
}

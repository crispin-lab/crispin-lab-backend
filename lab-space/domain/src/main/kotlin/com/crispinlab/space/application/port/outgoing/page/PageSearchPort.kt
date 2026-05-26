package com.crispinlab.space.application.port.outgoing.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
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
        fun allows(
            visibility: Visibility,
            spaceId: SpaceId,
            authorId: UserId
        ): Boolean

        data object Anonymous : VisibilityScope {
            override fun allows(
                visibility: Visibility,
                spaceId: SpaceId,
                authorId: UserId
            ): Boolean = visibility == Visibility.PUBLIC
        }

        data class Authenticated(
            val viewerId: UserId,
            val memberOfSpaceIds: Set<SpaceId>
        ) : VisibilityScope {
            override fun allows(
                visibility: Visibility,
                spaceId: SpaceId,
                authorId: UserId
            ): Boolean =
                when (visibility) {
                    Visibility.PUBLIC -> true
                    Visibility.INTERNAL -> spaceId in memberOfSpaceIds
                    Visibility.DRAFT -> authorId == viewerId
                }
        }

        data object Privileged : VisibilityScope {
            override fun allows(
                visibility: Visibility,
                spaceId: SpaceId,
                authorId: UserId
            ): Boolean = true
        }

        companion object {
            fun of(
                viewer: Viewer,
                memberOfSpaceIds: Set<SpaceId>
            ): VisibilityScope =
                when {
                    viewer.isAdmin -> Privileged
                    viewer is Viewer.Member -> Authenticated(viewer.userId, memberOfSpaceIds)
                    else -> Anonymous
                }
        }
    }

    data class PageSummary(
        val id: PageId,
        val spaceId: SpaceId,
        val title: String,
        val updatedAt: Instant
    )
}

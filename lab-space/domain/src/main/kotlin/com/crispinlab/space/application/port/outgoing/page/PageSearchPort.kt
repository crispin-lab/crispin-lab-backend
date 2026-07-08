package com.crispinlab.space.application.port.outgoing.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface PageSearchPort {
    fun search(
        keyword: String?,
        spaceId: SpaceId?,
        tagIds: Collection<TagId>,
        tagIdsAnyOf: Collection<TagId>,
        parentPageId: PageId?,
        onlyRoot: Boolean,
        sort: SortOption,
        scope: VisibilityScope,
        pageRequest: PageRequest
    ): PageResult<PageSummary>

    fun statsBySpaceIds(
        spaceIds: Collection<SpaceId>,
        scope: VisibilityScope
    ): Map<SpaceId, PageStat>

    enum class SortOption {
        CREATED_AT,
        UPDATED_AT,
        RELEVANCE,
        TREE;

        companion object {
            fun String.asSortOption(): SortOption =
                entries.firstOrNull { it.name == uppercase() }
                    ?: throw IllegalArgumentException("지원하지 않는 정렬 옵션입니다.")
        }
    }

    sealed interface VisibilityScope {
        fun allows(
            pageVisibility: Visibility,
            spaceVisibility: SpaceVisibility,
            spaceId: SpaceId,
            authorId: UserId
        ): Boolean

        data object Anonymous : VisibilityScope {
            override fun allows(
                pageVisibility: Visibility,
                spaceVisibility: SpaceVisibility,
                spaceId: SpaceId,
                authorId: UserId
            ): Boolean = effectiveOf(pageVisibility, spaceVisibility) == Visibility.PUBLIC
        }

        data class Authenticated(
            val viewerId: UserId,
            val memberOfSpaceIds: Set<SpaceId>
        ) : VisibilityScope {
            override fun allows(
                pageVisibility: Visibility,
                spaceVisibility: SpaceVisibility,
                spaceId: SpaceId,
                authorId: UserId
            ): Boolean =
                when (effectiveOf(pageVisibility, spaceVisibility)) {
                    Visibility.PUBLIC -> true
                    Visibility.MEMBER -> spaceId in memberOfSpaceIds
                    Visibility.INTERNAL -> authorId == viewerId
                    Visibility.DRAFT -> authorId == viewerId
                }
        }

        data object Privileged : VisibilityScope {
            override fun allows(
                pageVisibility: Visibility,
                spaceVisibility: SpaceVisibility,
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

            internal fun effectiveOf(
                pageVisibility: Visibility,
                spaceVisibility: SpaceVisibility
            ): Visibility =
                Visibility.entries[
                    minOf(
                        pageVisibility.ordinal,
                        spaceVisibility.ceiling().ordinal
                    )
                ]
        }
    }

    data class PageSummary(
        val id: PageId,
        val spaceId: SpaceId,
        val parentPageId: PageId?,
        val authorId: UserId,
        val title: String,
        val visibility: Visibility,
        val displayOrder: Int,
        val updatedAt: Instant
    )

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

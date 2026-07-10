package com.crispinlab.space.application.port.outgoing.space

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import java.time.Instant

interface SpaceRepository {
    fun save(entity: Space): Space

    fun findBy(id: SpaceId): Space?

    fun findVisibility(id: SpaceId): SpaceVisibility?

    fun findPage(
        pageRequest: PageRequest,
        scope: SpaceVisibilityScope,
        keyword: String? = null,
        sort: SortOption = SortOption.LAST_ACTIVITY_AT,
        direction: SortDirection = sort.defaultDirection
    ): PageResult<Summary>

    fun delete(id: SpaceId)

    data class Summary(
        val spaceId: SpaceId,
        val name: String,
        val description: String,
        val visibility: SpaceVisibility,
        val lastActivityAt: Instant,
        val createdAt: Instant,
        val updatedAt: Instant
    )

    enum class SortOption(
        val defaultDirection: SortDirection
    ) {
        LAST_ACTIVITY_AT(SortDirection.DESC),
        CREATED_AT(SortDirection.DESC),
        NAME(SortDirection.ASC)
        ;

        companion object {
            fun String.asSortOption(): SortOption =
                entries.firstOrNull { it.name == uppercase() }
                    ?: throw IllegalArgumentException("지원하지 않는 정렬 옵션입니다.")
        }
    }

    enum class SortDirection {
        ASC,
        DESC
        ;

        companion object {
            fun String.asSortDirection(): SortDirection =
                entries.firstOrNull { it.name == uppercase() }
                    ?: throw IllegalArgumentException("지원하지 않는 정렬 방향입니다.")
        }
    }
}

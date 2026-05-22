package com.crispinlab.space.adapter.persistence.space

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.persistence.ExposedEntityRepository
import com.crispinlab.space.adapter.persistence.toPageResult
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.space.SpaceVisibility.Companion.asSpaceVisibility
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedSpaceRepository :
    ExposedEntityRepository<Space, SpaceId>(),
    SpaceRepository {
    override val table = Spaces
    override val idColumn = Spaces.id
    override val deletedAtColumn = Spaces.deletedAt
    override val updateExclude = listOf(Spaces.id, Spaces.createdAt, Spaces.deletedAt)

    @Suppress("RedundantOverride")
    override fun delete(id: SpaceId) = super.delete(id)

    override fun ResultRow.toEntity(): Space =
        Space(
            id = SpaceId(this[Spaces.id]),
            name = this[Spaces.name],
            description = this[Spaces.description],
            visibility = decodeVisibility(this[Spaces.visibility]),
            createdAt = this[Spaces.createdAt],
            updatedAt = this[Spaces.updatedAt],
            deletedAt = this[Spaces.deletedAt]
        )

    override fun upsertBody(
        builder: UpsertStatement<Long>,
        entity: Space
    ) {
        builder[Spaces.id] = entity.id.value
        builder[Spaces.name] = entity.name
        builder[Spaces.description] = entity.description
        builder[Spaces.visibility] = entity.visibility.name
        builder[Spaces.createdAt] = entity.createdAt
        builder[Spaces.updatedAt] = entity.updatedAt
        builder[Spaces.deletedAt] = entity.deletedAt
    }

    override fun findPage(pageRequest: PageRequest): PageResult<Space> =
        Spaces
            .selectAll()
            .where { notDeleted() }
            .toPageResult(
                pageRequest,
                Spaces.createdAt to SortOrder.DESC,
                Spaces.id to SortOrder.DESC
            ) { it.toEntity() }

    private fun decodeVisibility(stored: String): SpaceVisibility =
        runCatching { stored.asSpaceVisibility() }
            .getOrElse { cause ->
                throw IllegalStateException("저장된 visibility 값을 해석할 수 없습니다.", cause)
            }
}

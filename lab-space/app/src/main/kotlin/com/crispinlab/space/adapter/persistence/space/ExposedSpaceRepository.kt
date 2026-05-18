package com.crispinlab.space.adapter.persistence.space

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.adapter.persistence.ExposedEntityRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

@Repository
class ExposedSpaceRepository :
    ExposedEntityRepository<Space, SpaceId>(),
    SpaceRepository {
    override val table = Spaces
    override val idColumn = Spaces.id
    override val deletedAtColumn = Spaces.deletedAt

    @Suppress("RedundantOverride")
    override fun delete(id: SpaceId) = super.delete(id)

    override fun ResultRow.toEntity(): Space =
        Space(
            id = SpaceId(this[Spaces.id]),
            name = this[Spaces.name],
            description = this[Spaces.description],
            createdAt = this[Spaces.createdAt],
            updatedAt = this[Spaces.updatedAt],
            deletedAt = this[Spaces.deletedAt]
        )

    override fun insert(entity: Space) {
        Spaces.insert {
            it[id] = entity.id.value
            it[name] = entity.name
            it[description] = entity.description
            it[createdAt] = entity.createdAt
            it[updatedAt] = entity.updatedAt
            it[deletedAt] = entity.deletedAt
        }
    }

    override fun update(entity: Space) {
        Spaces.update({ Spaces.id eq entity.id.value }) {
            it[name] = entity.name
            it[description] = entity.description
            it[updatedAt] = entity.updatedAt
            it[deletedAt] = entity.deletedAt
        }
    }

    override fun findPage(pageRequest: PageRequest): PageResult<Space> {
        val query = Spaces.selectAll().where { notDeleted() }
        val totalElements: Long = query.count()
        val items: List<Space> =
            query
                .orderBy(
                    Spaces.createdAt to SortOrder.DESC,
                    Spaces.id to SortOrder.DESC
                ).limit(pageRequest.size)
                .offset(pageRequest.offset)
                .map { it.toEntity() }
        return PageResult(
            items = items,
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = totalElements
        )
    }
}

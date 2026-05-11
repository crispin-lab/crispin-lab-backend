package com.crispinlab.space.adapter.persistence.space

import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

@Repository
class ExposedSpaceRepository : SpaceRepository {
    override fun save(space: Space): Space =
        Spaces
            .selectAll()
            .where { Spaces.id eq space.id.value }
            .firstOrNull()
            ?.let { update(space) }
            ?: insert(space)

    override fun findBy(id: SpaceId): Space? =
        Spaces
            .selectAll()
            .where { Spaces.id eq id.value }
            .firstOrNull()
            ?.toEntity()

    override fun delete(id: SpaceId) {
        Spaces.deleteWhere { Spaces.id eq id.value }
    }

    private fun insert(space: Space): Space =
        space.also {
            Spaces.insert {
                it[id] = space.id.value
                it[name] = space.name
                it[description] = space.description
                it[createdAt] = space.createdAt
                it[updatedAt] = space.updatedAt
            }
        }

    private fun update(space: Space): Space =
        space.also {
            Spaces.update({ Spaces.id eq space.id.value }) {
                it[name] = space.name
                it[description] = space.description
                it[updatedAt] = space.updatedAt
            }
        }

    private fun ResultRow.toEntity(): Space =
        Space(
            id = SpaceId(this[Spaces.id]),
            name = this[Spaces.name],
            description = this[Spaces.description],
            createdAt = this[Spaces.createdAt],
            updatedAt = this[Spaces.updatedAt]
        )
}

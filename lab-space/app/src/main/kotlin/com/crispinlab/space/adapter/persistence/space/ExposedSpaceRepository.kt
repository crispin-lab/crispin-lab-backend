package com.crispinlab.space.adapter.persistence.space

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
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

    override fun findPage(pageRequest: PageRequest): PageResult<Space> {
        /*
         * createdAt 충돌 시 동일 행이 페이지 경계에서 중복·누락되지 않도록 id 를 tiebreaker 로 둔다.
         * count 와 select 는 같은 트랜잭션 안이지만 격리 수준에 따라 미세한 skew 가능 — 현 스코프(단일
         * admin) 에서는 허용. 향후 keyset pagination / SERIALIZABLE 검토.
         */
        val totalElements: Long = Spaces.selectAll().count()
        val items: List<Space> =
            Spaces
                .selectAll()
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

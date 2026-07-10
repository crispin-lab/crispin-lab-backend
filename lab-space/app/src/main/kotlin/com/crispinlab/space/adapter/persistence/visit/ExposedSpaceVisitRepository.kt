package com.crispinlab.space.adapter.persistence.visit

import com.crispinlab.space.application.port.outgoing.visit.SpaceVisitRepository
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.visit.SpaceVisit
import com.crispinlab.space.domain.visit.SpaceVisitId
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import org.springframework.stereotype.Repository

@Repository
class ExposedSpaceVisitRepository : SpaceVisitRepository {
    override fun save(entity: SpaceVisit) {
        SpaceVisits.upsert(
            SpaceVisits.userId,
            SpaceVisits.spaceId,
            onUpdateExclude =
                listOf(
                    SpaceVisits.id,
                    SpaceVisits.userId,
                    SpaceVisits.spaceId
                )
        ) {
            it[SpaceVisits.id] = entity.id.value
            it[SpaceVisits.userId] = entity.userId.value
            it[SpaceVisits.spaceId] = entity.spaceId.value
            it[SpaceVisits.lastVisitedAt] = entity.lastVisitedAt
        }
    }

    override fun findByUserIdAndSpaceId(
        userId: UserId,
        spaceId: SpaceId
    ): SpaceVisit? =
        SpaceVisits
            .selectAll()
            .where {
                (SpaceVisits.userId eq userId.value) and (SpaceVisits.spaceId eq spaceId.value)
            }.firstOrNull()
            ?.toEntity()

    override fun findByUserIdAndSpaceIds(
        userId: UserId,
        spaceIds: Collection<SpaceId>
    ): Map<SpaceId, SpaceVisit> {
        if (spaceIds.isEmpty()) return emptyMap()
        val rawSpaceIds = spaceIds.map { it.value }.distinct()
        return SpaceVisits
            .selectAll()
            .where {
                (SpaceVisits.userId eq userId.value) and (SpaceVisits.spaceId inList rawSpaceIds)
            }.associate {
                SpaceId(it[SpaceVisits.spaceId]) to it.toEntity()
            }
    }

    private fun ResultRow.toEntity(): SpaceVisit =
        SpaceVisit(
            id = SpaceVisitId(this[SpaceVisits.id]),
            userId = UserId(this[SpaceVisits.userId]),
            spaceId = SpaceId(this[SpaceVisits.spaceId]),
            lastVisitedAt = this[SpaceVisits.lastVisitedAt]
        )
}

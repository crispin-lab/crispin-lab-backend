package com.crispinlab.space.adapter.persistence.spacemember

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.persistence.ExposedEntityRepository
import com.crispinlab.space.adapter.persistence.toPageResult
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMember
import com.crispinlab.space.domain.spacemember.SpaceMemberErrorCode
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.domain.spacemember.SpaceMemberRole.Companion.asSpaceMemberRole
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

private const val UNIQUE_VIOLATION_SQL_STATE = "23505"

@Repository
class ExposedSpaceMemberRepository :
    ExposedEntityRepository<SpaceMember, SpaceMemberId>(),
    SpaceMemberRepository {
    override val table = SpaceMembers
    override val idColumn = SpaceMembers.id
    override val deletedAtColumn = null
    override val updateExclude =
        listOf(
            SpaceMembers.id,
            SpaceMembers.spaceId,
            SpaceMembers.userId,
            SpaceMembers.joinedAt
        )

    override fun ResultRow.toEntity(): SpaceMember =
        SpaceMember(
            id = SpaceMemberId(this[SpaceMembers.id]),
            spaceId = SpaceId(this[SpaceMembers.spaceId]),
            userId = UserId(this[SpaceMembers.userId]),
            role = decodeRole(this[SpaceMembers.role]),
            joinedAt = this[SpaceMembers.joinedAt]
        )

    @Suppress("RedundantOverride")
    override fun delete(id: SpaceMemberId) = super.delete(id)

    override fun save(entity: SpaceMember): SpaceMember =
        try {
            super.save(entity)
        } catch (e: ExposedSQLException) {
            if (e.sqlState == UNIQUE_VIOLATION_SQL_STATE) {
                throw ConflictException(SpaceMemberErrorCode.ALREADY_JOINED, cause = e)
            }
            throw e
        }

    override fun upsertBody(
        builder: UpsertStatement<Long>,
        entity: SpaceMember
    ) {
        builder[SpaceMembers.id] = entity.id.value
        builder[SpaceMembers.spaceId] = entity.spaceId.value
        builder[SpaceMembers.userId] = entity.userId.value
        builder[SpaceMembers.role] = entity.role.name
        builder[SpaceMembers.joinedAt] = entity.joinedAt
    }

    override fun findBySpaceIdAndUserId(
        spaceId: SpaceId,
        userId: UserId
    ): SpaceMember? =
        SpaceMembers
            .selectAll()
            .where {
                (SpaceMembers.spaceId eq spaceId.value) and (SpaceMembers.userId eq userId.value)
            }.firstOrNull()
            ?.toEntity()

    override fun findBySpaceId(
        spaceId: SpaceId,
        pageRequest: PageRequest
    ): PageResult<SpaceMember> =
        SpaceMembers
            .selectAll()
            .where { SpaceMembers.spaceId eq spaceId.value }
            .toPageResult(
                pageRequest,
                SpaceMembers.joinedAt to SortOrder.ASC,
                SpaceMembers.id to SortOrder.ASC
            ) { it.toEntity() }

    override fun findSpaceIdsByUserId(userId: UserId): Set<SpaceId> =
        SpaceMembers
            .select(SpaceMembers.spaceId)
            .where { SpaceMembers.userId eq userId.value }
            .map { SpaceId(it[SpaceMembers.spaceId]) }
            .toSet()

    override fun countOwnersBy(spaceId: SpaceId): Long =
        SpaceMembers
            .select(SpaceMembers.id)
            .where {
                (SpaceMembers.spaceId eq spaceId.value) and
                    (SpaceMembers.role eq SpaceMemberRole.OWNER.name)
            }.count()

    private fun decodeRole(stored: String): SpaceMemberRole =
        runCatching { stored.asSpaceMemberRole() }
            .getOrElse { cause ->
                throw IllegalStateException("저장된 멤버 역할을 해석할 수 없습니다.", cause)
            }
}

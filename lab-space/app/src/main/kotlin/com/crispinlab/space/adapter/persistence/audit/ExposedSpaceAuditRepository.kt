package com.crispinlab.space.adapter.persistence.audit

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.persistence.ExposedEntityRepository
import com.crispinlab.space.adapter.persistence.toPageResult
import com.crispinlab.space.application.port.outgoing.audit.SpaceAuditRepository
import com.crispinlab.space.domain.audit.AuditChangeSummary
import com.crispinlab.space.domain.audit.SpaceAuditAction
import com.crispinlab.space.domain.audit.SpaceAuditAction.Companion.asSpaceAuditAction
import com.crispinlab.space.domain.audit.SpaceAuditEntry
import com.crispinlab.space.domain.audit.SpaceAuditEntryId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedSpaceAuditRepository :
    ExposedEntityRepository<SpaceAuditEntry, SpaceAuditEntryId>(),
    SpaceAuditRepository {
    override val table = SpaceAuditEntries
    override val idColumn = SpaceAuditEntries.id
    override val deletedAtColumn = null
    override val updateExclude =
        listOf(
            SpaceAuditEntries.id,
            SpaceAuditEntries.spaceId,
            SpaceAuditEntries.actorUserId,
            SpaceAuditEntries.action,
            SpaceAuditEntries.createdAt
        )

    override fun ResultRow.toEntity(): SpaceAuditEntry =
        SpaceAuditEntry(
            id = SpaceAuditEntryId(this[SpaceAuditEntries.id]),
            spaceId = SpaceId(this[SpaceAuditEntries.spaceId]),
            actorUserId = UserId(this[SpaceAuditEntries.actorUserId]),
            action = decodeAction(this[SpaceAuditEntries.action]),
            changeSummary = AuditChangeSummary(this[SpaceAuditEntries.changeSummary]),
            createdAt = this[SpaceAuditEntries.createdAt]
        )

    override fun upsertBody(
        builder: UpsertStatement<Long>,
        entity: SpaceAuditEntry
    ) {
        builder[SpaceAuditEntries.id] = entity.id.value
        builder[SpaceAuditEntries.spaceId] = entity.spaceId.value
        builder[SpaceAuditEntries.actorUserId] = entity.actorUserId.value
        builder[SpaceAuditEntries.action] = entity.action.name
        builder[SpaceAuditEntries.changeSummary] = entity.changeSummary.json
        builder[SpaceAuditEntries.createdAt] = entity.createdAt
    }

    override fun findBySpaceId(
        spaceId: SpaceId,
        pageRequest: PageRequest
    ): PageResult<SpaceAuditEntry> =
        SpaceAuditEntries
            .selectAll()
            .where { SpaceAuditEntries.spaceId eq spaceId.value }
            .toPageResult(
                pageRequest,
                SpaceAuditEntries.createdAt to SortOrder.DESC,
                SpaceAuditEntries.id to SortOrder.DESC
            ) { it.toEntity() }

    private fun decodeAction(stored: String): SpaceAuditAction =
        runCatching { stored.asSpaceAuditAction() }
            .getOrElse { cause ->
                throw IllegalStateException("저장된 감사 이력 종류를 해석할 수 없습니다.", cause)
            }
}

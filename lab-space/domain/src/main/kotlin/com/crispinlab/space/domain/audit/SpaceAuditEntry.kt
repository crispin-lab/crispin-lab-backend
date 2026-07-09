package com.crispinlab.space.domain.audit

import com.crispinlab.common.domain.Entity
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant
import java.time.Instant.now

class SpaceAuditEntry(
    override val id: SpaceAuditEntryId,
    val spaceId: SpaceId,
    val actorUserId: UserId,
    val action: SpaceAuditAction,
    val changeSummary: AuditChangeSummary,
    val createdAt: Instant = now()
) : Entity<SpaceAuditEntryId>

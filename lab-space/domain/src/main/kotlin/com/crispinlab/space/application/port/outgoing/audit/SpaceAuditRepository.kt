package com.crispinlab.space.application.port.outgoing.audit

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.domain.audit.SpaceAuditEntry
import com.crispinlab.space.domain.audit.SpaceAuditEntryId
import com.crispinlab.space.domain.space.SpaceId

interface SpaceAuditRepository {
    fun save(entity: SpaceAuditEntry): SpaceAuditEntry

    fun findBy(id: SpaceAuditEntryId): SpaceAuditEntry?

    fun findBySpaceId(
        spaceId: SpaceId,
        pageRequest: PageRequest
    ): PageResult<SpaceAuditEntry>
}

package com.crispinlab.space.domain.audit

import com.crispinlab.common.domain.EntityId

data class SpaceAuditEntryId(
    override val value: Long
) : EntityId {
    companion object {
        fun String.asSpaceAuditEntryId(): SpaceAuditEntryId =
            SpaceAuditEntryId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("감사 이력 ID 형식이 올바르지 않습니다.")
            )
    }
}

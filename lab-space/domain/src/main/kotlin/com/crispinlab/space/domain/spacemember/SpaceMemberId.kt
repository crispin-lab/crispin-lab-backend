package com.crispinlab.space.domain.spacemember

import com.crispinlab.common.domain.EntityId

data class SpaceMemberId(
    override val value: Long
) : EntityId {
    companion object {
        fun String.asSpaceMemberId(): SpaceMemberId =
            SpaceMemberId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("스페이스 멤버 ID 형식이 올바르지 않습니다.")
            )
    }
}

package com.crispinlab.space.domain.space

import com.crispinlab.common.domain.EntityId

data class SpaceId(
    override val value: Long
) : EntityId {
    companion object {
        fun String.asSpaceId(): SpaceId =
            SpaceId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("스페이스 ID 형식이 올바르지 않습니다.")
            )
    }
}

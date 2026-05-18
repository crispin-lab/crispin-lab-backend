package com.crispinlab.space.domain.user

import com.crispinlab.common.domain.EntityId

data class UserId(
    override val value: Long
) : EntityId {
    companion object {
        fun String.asUserId(): UserId =
            UserId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("사용자 ID 형식이 올바르지 않습니다.")
            )
    }
}

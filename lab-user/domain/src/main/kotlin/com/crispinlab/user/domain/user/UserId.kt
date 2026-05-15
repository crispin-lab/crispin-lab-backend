package com.crispinlab.user.domain.user

@JvmInline
value class UserId(
    val value: Long
) {
    companion object {
        fun String.asUserId(): UserId =
            UserId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("사용자 ID 형식이 올바르지 않습니다.")
            )
    }
}

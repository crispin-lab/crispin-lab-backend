package com.crispinlab.space.domain.space

@JvmInline
value class SpaceId(
    val value: Long
) {
    companion object {
        fun String.asSpaceId(): SpaceId =
            SpaceId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("스페이스 ID 형식이 올바르지 않습니다.")
            )
    }
}

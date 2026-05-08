package com.crispinlab.space.domain.tag

@JvmInline
value class TagId(
    val value: Long
) {
    companion object {
        fun String.asTagId(): TagId =
            TagId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("태그 ID 형식이 올바르지 않습니다.")
            )
    }
}

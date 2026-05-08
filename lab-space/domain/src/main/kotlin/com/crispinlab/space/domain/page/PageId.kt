package com.crispinlab.space.domain.page

@JvmInline
value class PageId(
    val value: Long
) {
    companion object {
        fun String.asPageId(): PageId =
            PageId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("페이지 ID 형식이 올바르지 않습니다.")
            )
    }
}

package com.crispinlab.space.domain.page

@JvmInline
value class PageRevisionId(
    val value: Long
) {
    companion object {
        fun String.asPageRevisionId(): PageRevisionId =
            PageRevisionId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("페이지 리비전 ID 형식이 올바르지 않습니다.")
            )
    }
}

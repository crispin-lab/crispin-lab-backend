package com.crispinlab.space.domain.page

@JvmInline
value class PageLinkId(
    val value: Long
) {
    companion object {
        fun String.asPageLinkId(): PageLinkId =
            PageLinkId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("페이지 링크 ID 형식이 올바르지 않습니다.")
            )
    }
}

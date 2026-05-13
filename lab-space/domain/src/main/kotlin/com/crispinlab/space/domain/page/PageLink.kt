package com.crispinlab.space.domain.page

import java.time.Instant

data class PageLink(
    val id: PageLinkId,
    val pageId: PageId,
    val revisionId: PageRevisionId,
    val target: String,
    val type: Type,
    val createdAt: Instant
) {
    init {
        require(target.isNotBlank()) {
            "링크 대상이 비어 있습니다."
        }
        require(target.length <= MAX_TARGET_LENGTH) {
            "링크 대상은 ${MAX_TARGET_LENGTH}자를 넘을 수 없습니다."
        }
    }

    enum class Type {
        INTERNAL,
        EXTERNAL;

        companion object {
            fun String.asType(): Type =
                entries.firstOrNull { it.name == uppercase() }
                    ?: throw IllegalArgumentException("지원하지 않는 링크 타입입니다.")
        }
    }

    companion object {
        const val MAX_TARGET_LENGTH: Int = 500
    }
}

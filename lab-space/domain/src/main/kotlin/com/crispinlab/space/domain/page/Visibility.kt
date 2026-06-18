package com.crispinlab.space.domain.page

enum class Visibility {
    DRAFT,
    INTERNAL,
    MEMBER,
    PUBLIC;

    companion object {
        fun String.asVisibility(): Visibility =
            entries.firstOrNull { it.name == uppercase() }
                ?: throw IllegalArgumentException("지원하지 않는 공개 범위입니다.")
    }
}

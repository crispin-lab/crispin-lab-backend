package com.crispinlab.space.domain.space

import com.crispinlab.space.domain.page.Visibility

enum class SpaceVisibility {
    INTERNAL,
    PUBLIC
    ;

    fun ceiling(): Visibility =
        when (this) {
            PUBLIC -> Visibility.PUBLIC
            INTERNAL -> Visibility.MEMBER
        }

    companion object {
        fun String.asSpaceVisibility(): SpaceVisibility =
            entries.firstOrNull { it.name == uppercase() }
                ?: throw IllegalArgumentException("지원하지 않는 공개 범위입니다.")
    }
}

package com.crispinlab.space.domain.space

enum class SpaceVisibility {
    INTERNAL,
    PUBLIC
    ;

    companion object {
        fun String.asSpaceVisibility(): SpaceVisibility =
            entries.firstOrNull { it.name == uppercase() }
                ?: throw IllegalArgumentException("지원하지 않는 공개 범위입니다.")
    }
}

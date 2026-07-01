package com.crispinlab.notification.domain.notification

enum class SourceType {
    PAGE,
    COMMENT
    ;

    companion object {
        fun String.asSourceType(): SourceType =
            entries.firstOrNull { it.name == uppercase() }
                ?: throw IllegalArgumentException("지원하지 않는 알림 source type 입니다.")
    }
}

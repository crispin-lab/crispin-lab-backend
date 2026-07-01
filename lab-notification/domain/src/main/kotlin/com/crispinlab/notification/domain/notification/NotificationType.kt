package com.crispinlab.notification.domain.notification

enum class NotificationType {
    MENTION
    ;

    companion object {
        fun String.asNotificationType(): NotificationType =
            entries.firstOrNull { it.name == uppercase() }
                ?: throw IllegalArgumentException("지원하지 않는 알림 타입입니다.")
    }
}

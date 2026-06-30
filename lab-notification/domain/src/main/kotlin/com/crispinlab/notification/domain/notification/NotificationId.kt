package com.crispinlab.notification.domain.notification

import com.crispinlab.common.domain.EntityId

data class NotificationId(
    override val value: Long
) : EntityId {
    companion object {
        fun String.asNotificationId(): NotificationId =
            NotificationId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("알림 ID 형식이 올바르지 않습니다.")
            )
    }
}

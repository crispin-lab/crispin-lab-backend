package com.crispinlab.notification.domain.notification

import com.crispinlab.common.exception.ErrorCode

enum class NotificationErrorCode(
    override val defaultMessage: String
) : ErrorCode {
    NOTIFICATION_NOT_FOUND("알림을 찾을 수 없습니다.")
    ;

    override val code: String get() = name
}

package com.crispinlab.user.domain.user

import com.crispinlab.common.exception.ErrorCode

enum class UserErrorCode(
    override val defaultMessage: String
) : ErrorCode {
    USER_NOT_FOUND("사용자를 찾을 수 없습니다.")
    ;

    override val code: String get() = name
}

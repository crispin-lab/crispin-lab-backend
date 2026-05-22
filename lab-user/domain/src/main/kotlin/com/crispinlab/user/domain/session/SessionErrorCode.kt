package com.crispinlab.user.domain.session

import com.crispinlab.common.exception.ErrorCode

enum class SessionErrorCode(
    override val defaultMessage: String
) : ErrorCode {
    INVALID_SESSION("세션이 유효하지 않습니다.")
    ;

    override val code: String get() = name
}

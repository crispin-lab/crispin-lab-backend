package com.crispinlab.user.domain.user

import com.crispinlab.common.exception.ErrorCode

enum class UserErrorCode(
    override val defaultMessage: String
) : ErrorCode {
    USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
    EMAIL_DUPLICATED("이미 등록된 이메일입니다."),
    HANDLE_DUPLICATED("이미 등록된 핸들입니다."),
    INVALID_CREDENTIALS("아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_SESSION("세션이 만료되었습니다.")
    ;

    override val code: String get() = name
}

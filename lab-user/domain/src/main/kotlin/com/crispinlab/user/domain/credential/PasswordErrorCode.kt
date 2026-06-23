package com.crispinlab.user.domain.credential

import com.crispinlab.common.exception.ErrorCode

enum class PasswordErrorCode(
    override val defaultMessage: String
) : ErrorCode {
    PASSWORD_TOO_SHORT("비밀번호는 8자 이상이어야 합니다."),
    PASSWORD_TOO_LONG("비밀번호는 72자를 넘을 수 없습니다."),
    PASSWORD_INSUFFICIENT_VARIETY("비밀번호는 영문, 숫자, 특수문자 중 두 종류 이상을 포함해야 합니다."),
    PASSWORD_CONTAINS_WHITESPACE("비밀번호 양끝에는 공백을 포함할 수 없습니다."),
    PASSWORD_SIMILAR_TO_IDENTITY("비밀번호에 이메일 또는 사용자 이름을 포함할 수 없습니다."),
    PASSWORD_BLOCKED("흔하게 사용되는 비밀번호입니다. 다른 비밀번호를 사용해 주세요.")
    ;

    override val code: String get() = name
}

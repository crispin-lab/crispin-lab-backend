package com.crispinlab.space.domain.space

import com.crispinlab.common.exception.ErrorCode

enum class SpaceErrorCode(
    override val defaultMessage: String
) : ErrorCode {
    SPACE_NOT_FOUND("스페이스를 찾을 수 없습니다."),
    SPACE_ADMIN_ONLY("관리자만 수행할 수 있습니다.")
    ;

    override val code: String get() = name
}

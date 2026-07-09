package com.crispinlab.space.domain.audit

import com.crispinlab.common.exception.ErrorCode

enum class SpaceAuditErrorCode(
    override val defaultMessage: String
) : ErrorCode {
    SPACE_AUDIT_ENTRY_NOT_FOUND("감사 이력을 찾을 수 없습니다.")
    ;

    override val code: String get() = name
}

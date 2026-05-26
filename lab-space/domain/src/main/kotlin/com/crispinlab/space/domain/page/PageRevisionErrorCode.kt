package com.crispinlab.space.domain.page

import com.crispinlab.common.exception.ErrorCode

enum class PageRevisionErrorCode(
    override val defaultMessage: String
) : ErrorCode {
    PAGE_REVISION_NOT_FOUND("페이지 리비전을 찾을 수 없습니다.")
    ;

    override val code: String get() = name
}

package com.crispinlab.space.domain.page

import com.crispinlab.common.exception.ErrorCode

enum class PageErrorCode(
    override val defaultMessage: String
) : ErrorCode {
    PAGE_NOT_FOUND("페이지를 찾을 수 없습니다."),
    PARENT_PAGE_NOT_FOUND("부모 페이지를 찾을 수 없습니다."),
    PAGE_VISIBILITY_EXCEEDS_SPACE("페이지 공개 범위는 상위 스페이스보다 넓을 수 없습니다.")
    ;

    override val code: String get() = name
}

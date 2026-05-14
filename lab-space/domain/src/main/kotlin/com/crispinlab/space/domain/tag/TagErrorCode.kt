package com.crispinlab.space.domain.tag

import com.crispinlab.common.exception.ErrorCode

enum class TagErrorCode(
    override val defaultMessage: String
) : ErrorCode {
    TAG_NOT_FOUND("태그를 찾을 수 없습니다."),
    TAG_NAME_DUPLICATED("이미 등록된 태그 이름입니다.")
    ;

    override val code: String get() = name
}

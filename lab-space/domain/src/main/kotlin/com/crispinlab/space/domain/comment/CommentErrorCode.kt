package com.crispinlab.space.domain.comment

import com.crispinlab.common.exception.ErrorCode

enum class CommentErrorCode(
    override val defaultMessage: String
) : ErrorCode {
    COMMENT_NOT_FOUND("댓글을 찾을 수 없습니다."),
    COMMENT_NOT_ALLOWED("댓글을 남길 수 없습니다.")
    ;

    override val code: String get() = name
}

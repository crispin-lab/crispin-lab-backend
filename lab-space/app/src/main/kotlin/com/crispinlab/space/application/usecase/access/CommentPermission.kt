package com.crispinlab.space.application.usecase.access

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentErrorCode

internal fun Viewer.canComment(): Boolean = isAuthenticated

internal fun Viewer.Member.requireCommentPermission() {
    if (!canComment()) {
        throw ForbiddenException(CommentErrorCode.COMMENT_NOT_ALLOWED)
    }
}

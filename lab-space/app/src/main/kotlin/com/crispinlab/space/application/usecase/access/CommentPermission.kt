package com.crispinlab.space.application.usecase.access

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentErrorCode
import com.crispinlab.user.domain.user.UserId

internal fun Viewer.canComment(): Boolean = isAuthenticated

internal fun Viewer.Member.requireCommentPermission() {
    if (!canComment()) {
        throw ForbiddenException(CommentErrorCode.COMMENT_NOT_ALLOWED)
    }
}

internal fun Viewer.canEditCommentOf(authorId: UserId): Boolean =
    isAdmin || (this is Viewer.Member && userId == authorId)

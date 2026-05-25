package com.crispinlab.space.application.port.incoming.comment

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.comment.CommentDeleting.Request
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.comment.CommentId.Companion.asCommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId

interface CommentDeleting : UseCase<Request, Unit> {
    class Request(
        pageId: String,
        commentId: String,
        val viewer: Viewer.Member
    ) {
        val pageId: PageId = pageId.asPageId()
        val commentId: CommentId = commentId.asCommentId()
    }
}

package com.crispinlab.space.application.port.incoming.comment

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Request
import com.crispinlab.space.application.port.incoming.comment.CommentEditing.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.comment.CommentId.Companion.asCommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import java.time.Instant

interface CommentEditing : UseCase<Request, Result> {
    class Request(
        pageId: String,
        commentId: String,
        val body: String,
        val viewer: Viewer.Member
    ) {
        val pageId: PageId = pageId.asPageId()
        val commentId: CommentId = commentId.asCommentId()
    }

    data class Result(
        val commentId: CommentId,
        val authorHandle: String,
        val body: String,
        val updatedAt: Instant
    )
}

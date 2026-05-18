package com.crispinlab.space.application.port.incoming.comment

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Request
import com.crispinlab.space.application.port.incoming.comment.CommentGetting.Result
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.comment.CommentId.Companion.asCommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.user.UserId
import java.time.Instant

interface CommentGetting : UseCase<Request, Result> {
    class Request(
        pageId: String,
        commentId: String,
        val currentUserId: UserId
    ) {
        val pageId: PageId = pageId.asPageId()
        val commentId: CommentId = commentId.asCommentId()
    }

    data class Result(
        val commentId: CommentId,
        val pageId: PageId,
        val authorId: UserId,
        val body: String,
        val createdAt: Instant,
        val updatedAt: Instant,
        val deletedAt: Instant?
    )
}

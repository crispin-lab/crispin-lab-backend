package com.crispinlab.composition.application.port.incoming.comment

import com.crispinlab.common.application.UseCase
import com.crispinlab.composition.application.port.incoming.comment.CommentGettingComposition.Request
import com.crispinlab.composition.application.port.incoming.comment.CommentGettingComposition.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface CommentGettingComposition : UseCase<Request, Result> {
    class Request(
        val pageId: String,
        val commentId: String,
        val viewer: Viewer.Member
    )

    data class Result(
        val commentId: CommentId,
        val pageId: PageId,
        val authorId: UserId,
        val authorHandle: String,
        val content: String,
        val canEdit: Boolean,
        val createdAt: Instant,
        val updatedAt: Instant
    )
}

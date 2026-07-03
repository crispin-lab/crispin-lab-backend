package com.crispinlab.composition.application.port.incoming.comment

import com.crispinlab.common.application.UseCase
import com.crispinlab.composition.application.port.incoming.comment.CommentEditingComposition.Request
import com.crispinlab.composition.application.port.incoming.comment.CommentEditingComposition.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentId
import java.time.Instant

interface CommentEditingComposition : UseCase<Request, Result> {
    class Request(
        val pageId: String,
        val commentId: String,
        val content: String,
        val viewer: Viewer.Member
    )

    data class Result(
        val commentId: CommentId,
        val authorHandle: String,
        val content: String,
        val updatedAt: Instant
    )
}

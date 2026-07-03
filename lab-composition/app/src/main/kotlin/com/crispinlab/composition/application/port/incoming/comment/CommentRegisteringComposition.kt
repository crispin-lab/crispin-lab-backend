package com.crispinlab.composition.application.port.incoming.comment

import com.crispinlab.common.application.UseCase
import com.crispinlab.composition.application.port.incoming.comment.CommentRegisteringComposition.Request
import com.crispinlab.composition.application.port.incoming.comment.CommentRegisteringComposition.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentId

interface CommentRegisteringComposition : UseCase<Request, Result> {
    class Request(
        val pageId: String,
        val content: String,
        val viewer: Viewer.Member
    )

    data class Result(
        val commentId: CommentId,
        val authorHandle: String
    )
}

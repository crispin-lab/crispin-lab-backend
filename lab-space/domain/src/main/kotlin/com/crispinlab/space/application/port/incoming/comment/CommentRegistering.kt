package com.crispinlab.space.application.port.incoming.comment

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Request
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentContent
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId

interface CommentRegistering : UseCase<Request, Result> {
    class Request(
        pageId: String,
        content: String,
        val viewer: Viewer.Member
    ) {
        val pageId: PageId = pageId.asPageId()
        val content: CommentContent = CommentContent(content)
    }

    data class Result(
        val commentId: CommentId,
        val authorHandle: String
    )
}

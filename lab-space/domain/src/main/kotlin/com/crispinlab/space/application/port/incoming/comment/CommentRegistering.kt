package com.crispinlab.space.application.port.incoming.comment

import com.crispinlab.common.application.UseCase
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Request
import com.crispinlab.space.application.port.incoming.comment.CommentRegistering.Result
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.user.UserId

interface CommentRegistering : UseCase<Request, Result> {
    class Request(
        pageId: String,
        val body: String,
        val currentUserId: UserId
    ) {
        val pageId: PageId = pageId.asPageId()
    }

    data class Result(
        val commentId: String
    )
}

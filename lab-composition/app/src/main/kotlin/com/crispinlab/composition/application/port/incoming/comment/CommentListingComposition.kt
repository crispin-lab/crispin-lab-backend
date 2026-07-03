package com.crispinlab.composition.application.port.incoming.comment

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.composition.application.port.incoming.comment.CommentListingComposition.Request
import com.crispinlab.composition.application.port.incoming.comment.CommentListingComposition.Result
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface CommentListingComposition : UseCase<Request, PageResult<Result>> {
    class Request(
        val pageId: String,
        val page: Int,
        val size: Int,
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

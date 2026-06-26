package com.crispinlab.space.application.port.incoming.comment

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Request
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Summary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface CommentListing : UseCase<Request, PageResult<Summary>> {
    class Request(
        pageId: String,
        page: Int = 0,
        size: Int = DEFAULT_SIZE,
        val viewer: Viewer.Member
    ) {
        val pageId: PageId = pageId.asPageId()
        val pageRequest: PageRequest =
            PageRequest(
                page = page,
                size = size
            )
    }

    /*
    todo    :: 마스킹 정책 결정 시 findByPageIdIncludingDeleted 경로 + Summary.deletedAt 동시 도입.
     author :: heechoel shin
     date   :: 2026-05-18T00:00:00KST
     ticket :: LAB-23
     */
    data class Summary(
        val commentId: CommentId,
        val pageId: PageId,
        val authorId: UserId,
        val authorHandle: String,
        val body: String,
        val createdAt: Instant,
        val updatedAt: Instant
    )
}

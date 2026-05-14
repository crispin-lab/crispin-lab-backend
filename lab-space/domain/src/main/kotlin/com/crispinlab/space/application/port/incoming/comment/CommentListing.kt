package com.crispinlab.space.application.port.incoming.comment

import com.crispinlab.common.application.UseCase
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Request
import com.crispinlab.space.application.port.incoming.comment.CommentListing.Summary
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageId.Companion.asPageId
import com.crispinlab.space.domain.user.UserId
import java.time.Instant

interface CommentListing : UseCase<Request, PageResult<Summary>> {
    class Request(
        pageId: String,
        page: Int = 0,
        size: Int = DEFAULT_SIZE,
        val currentUserId: UserId
    ) {
        val pageId: PageId = pageId.asPageId()
        val pageRequest: PageRequest =
            PageRequest(
                page = page,
                size = size
            )
    }

    /*
    todo    :: 삭제된 댓글(deletedAt != null) 은 현재 body·작성자 정보를 그대로 노출한다. 마스킹 정책(예: body 를 "삭제된 댓글입니다" 로 치환) 결정 시 Summary 구조 또는 매핑을 조정한다.
     author :: heechoel shin
     date   :: 2026-05-14T00:00:00KST
     ticket :: LAB-23
     */
    data class Summary(
        val commentId: String,
        val pageId: String,
        val authorId: String,
        val body: String,
        val createdAt: Instant,
        val updatedAt: Instant,
        val deletedAt: Instant?
    )
}

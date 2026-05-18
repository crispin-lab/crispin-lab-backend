package com.crispinlab.space.application.port.outgoing.comment

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId

interface CommentRepository {
    fun save(entity: Comment): Comment

    fun findBy(id: CommentId): Comment?

    fun findByPageId(
        pageId: PageId,
        pageRequest: PageRequest
    ): PageResult<Comment>

    /**
     * Comment 가 `SoftDeletable` 이므로 어댑터의 base 가 자동으로 `deleted_at` UPDATE 로 동작 (`repository.md` 참조).
     */
    fun delete(id: CommentId)
}

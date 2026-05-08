package com.crispinlab.space.application.port.outgoing.comment

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId

interface CommentRepository {
    fun save(comment: Comment): Comment

    fun findBy(id: CommentId): Comment?

    fun findByPageId(
        pageId: PageId,
        pageRequest: PageRequest
    ): PageResult<Comment>

    fun delete(id: CommentId)
}

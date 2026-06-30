package com.crispinlab.space.domain.comment

import com.crispinlab.common.domain.Entity
import com.crispinlab.common.domain.SoftDeletable
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant
import java.time.Instant.now

class Comment(
    override val id: CommentId,
    val pageId: PageId,
    val authorId: UserId,
    content: CommentContent,
    val createdAt: Instant = now(),
    updatedAt: Instant = createdAt,
    deletedAt: Instant? = null
) : Entity<CommentId>,
    SoftDeletable {
    var content: CommentContent = content
        private set
    var updatedAt: Instant = updatedAt
        private set
    override var deletedAt: Instant? = deletedAt
        private set

    fun edit(content: CommentContent) {
        check(!isDeleted) {
            "삭제된 댓글은 수정할 수 없습니다."
        }
        this.content = content
        this.updatedAt = now()
    }
}

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
    body: String,
    val createdAt: Instant = now(),
    updatedAt: Instant = createdAt,
    deletedAt: Instant? = null
) : Entity<CommentId>,
    SoftDeletable {
    var body: String = body
        private set
    var updatedAt: Instant = updatedAt
        private set
    override var deletedAt: Instant? = deletedAt
        private set

    init {
        validateBody(body)
    }

    fun edit(body: String) {
        check(!isDeleted) {
            "삭제된 댓글은 수정할 수 없습니다."
        }
        validateBody(body)
        this.body = body
        this.updatedAt = now()
    }

    private fun validateBody(body: String) {
        require(body.isNotBlank()) {
            "댓글 내용을 입력해 주세요."
        }
        require(body.length <= MAX_BODY_LENGTH) {
            "댓글은 ${MAX_BODY_LENGTH}자를 넘을 수 없습니다."
        }
    }

    companion object {
        const val MAX_BODY_LENGTH: Int = 2_000
    }
}

package com.crispinlab.space.domain.comment

import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.user.UserId
import java.time.Instant

class Comment(
    val id: CommentId,
    val pageId: PageId,
    val authorId: UserId,
    body: String,
    val createdAt: Instant,
    updatedAt: Instant = createdAt,
    deletedAt: Instant? = null
) {
    var body: String = body
        private set
    var updatedAt: Instant = updatedAt
        private set
    var deletedAt: Instant? = deletedAt
        private set

    val isDeleted: Boolean
        get() = deletedAt != null

    init {
        validateBody(body)
    }

    fun edit(
        body: String,
        occurredAt: Instant
    ) {
        check(!isDeleted) {
            "삭제된 댓글은 수정할 수 없습니다."
        }
        validateBody(body)
        this.body = body
        this.updatedAt = occurredAt
    }

    fun delete(occurredAt: Instant) {
        check(!isDeleted) {
            "이미 삭제된 댓글입니다."
        }
        this.deletedAt = occurredAt
        this.updatedAt = occurredAt
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

package com.crispinlab.space.domain.comment

import com.crispinlab.common.domain.EntityId

data class CommentId(
    override val value: Long
) : EntityId {
    companion object {
        fun String.asCommentId(): CommentId =
            CommentId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("댓글 ID 형식이 올바르지 않습니다.")
            )
    }
}

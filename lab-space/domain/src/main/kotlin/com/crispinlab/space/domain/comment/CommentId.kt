package com.crispinlab.space.domain.comment

@JvmInline
value class CommentId(
    val value: Long
) {
    companion object {
        fun String.asCommentId(): CommentId =
            CommentId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("댓글 ID 형식이 올바르지 않습니다.")
            )
    }
}

package com.crispinlab.space.domain.comment

data class CommentContent(
    val raw: String
) {
    init {
        require(raw.isNotBlank()) {
            "댓글 내용을 입력해 주세요."
        }
        require(raw.length <= MAX_RAW_LENGTH) {
            "댓글은 ${MAX_RAW_LENGTH}자를 넘을 수 없습니다."
        }
    }

    companion object {
        const val MAX_RAW_LENGTH: Int = 5_000
    }
}

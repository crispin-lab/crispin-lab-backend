package com.crispinlab.space.domain.page

data class PageContent(
    val raw: String
) {
    init {
        require(raw.isNotBlank()) {
            "본문을 입력해 주세요."
        }
        require(raw.length <= MAX_RAW_LENGTH) {
            "본문은 ${MAX_RAW_LENGTH}자를 넘을 수 없습니다."
        }
    }

    companion object {
        const val MAX_RAW_LENGTH: Int = 100_000
    }
}

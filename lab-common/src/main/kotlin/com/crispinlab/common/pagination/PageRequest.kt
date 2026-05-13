package com.crispinlab.common.pagination

data class PageRequest(
    val page: Int,
    val size: Int
) {
    init {
        require(page >= 0) {
            "페이지 번호는 0 이상이어야 합니다."
        }
        require(size in 1..MAX_SIZE) {
            "페이지 크기는 1 이상 ${MAX_SIZE} 이하여야 합니다."
        }
    }

    val offset: Long
        get() = page.toLong() * size

    companion object {
        const val MAX_SIZE: Int = 200
        const val DEFAULT_SIZE: Int = 20

        fun firstPage(size: Int = DEFAULT_SIZE): PageRequest = PageRequest(page = 0, size = size)
    }
}

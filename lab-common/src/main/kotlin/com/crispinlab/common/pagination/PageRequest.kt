package com.crispinlab.common.pagination

data class PageRequest(
    val page: Int,
    val size: Int
) {
    init {
        require(page >= 0) { "page must be >= 0, was $page" }
        require(size in 1..MAX_SIZE) { "size must be in 1..$MAX_SIZE, was $size" }
    }

    val offset: Int
        get() = page * size

    companion object {
        const val MAX_SIZE: Int = 200
        const val DEFAULT_SIZE: Int = 20

        fun firstPage(size: Int = DEFAULT_SIZE): PageRequest = PageRequest(page = 0, size = size)
    }
}

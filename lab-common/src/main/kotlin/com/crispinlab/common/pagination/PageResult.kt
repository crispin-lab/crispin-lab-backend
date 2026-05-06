package com.crispinlab.common.pagination

data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long
) {
    val totalPages: Int
        get() = if (totalElements == 0L) 0 else ((totalElements + size - 1) / size).toInt()

    val hasNext: Boolean
        get() = page + 1 < totalPages

    val isEmpty: Boolean
        get() = items.isEmpty()

    fun <R> map(transform: (T) -> R): PageResult<R> =
        PageResult(
            items = items.map(transform),
            page = page,
            size = size,
            totalElements = totalElements
        )

    companion object {
        fun <T> empty(request: PageRequest): PageResult<T> =
            PageResult(
                items = emptyList(),
                page = request.page,
                size = request.size,
                totalElements = 0L
            )
    }
}

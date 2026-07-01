package com.crispinlab.notification.adapter.persistence

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.Query

fun <T> Query.toPageResult(
    pageRequest: PageRequest,
    vararg order: Pair<Expression<*>, SortOrder>,
    mapper: (ResultRow) -> T
): PageResult<T> {
    val totalElements: Long = count()
    val items: List<T> =
        orderBy(*order)
            .limit(pageRequest.size)
            .offset(pageRequest.offset)
            .map(mapper)
    return PageResult(
        items = items,
        page = pageRequest.page,
        size = pageRequest.size,
        totalElements = totalElements
    )
}

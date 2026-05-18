package com.crispinlab.space.domain.page

import com.crispinlab.common.domain.EntityId

data class PageRevisionId(
    override val value: Long
) : EntityId {
    companion object {
        fun String.asPageRevisionId(): PageRevisionId =
            PageRevisionId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("페이지 리비전 ID 형식이 올바르지 않습니다.")
            )
    }
}

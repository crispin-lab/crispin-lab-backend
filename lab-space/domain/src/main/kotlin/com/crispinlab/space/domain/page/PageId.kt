package com.crispinlab.space.domain.page

import com.crispinlab.common.domain.EntityId

data class PageId(
    override val value: Long
) : EntityId {
    companion object {
        fun String.asPageId(): PageId =
            PageId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("페이지 ID 형식이 올바르지 않습니다.")
            )
    }
}

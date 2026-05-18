package com.crispinlab.space.domain.page

import com.crispinlab.common.domain.EntityId

data class PageLinkId(
    override val value: Long
) : EntityId {
    companion object {
        fun String.asPageLinkId(): PageLinkId =
            PageLinkId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("페이지 링크 ID 형식이 올바르지 않습니다.")
            )
    }
}

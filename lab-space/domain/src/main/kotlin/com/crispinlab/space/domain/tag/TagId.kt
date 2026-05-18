package com.crispinlab.space.domain.tag

import com.crispinlab.common.domain.EntityId

data class TagId(
    override val value: Long
) : EntityId {
    companion object {
        fun String.asTagId(): TagId =
            TagId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("태그 ID 형식이 올바르지 않습니다.")
            )
    }
}

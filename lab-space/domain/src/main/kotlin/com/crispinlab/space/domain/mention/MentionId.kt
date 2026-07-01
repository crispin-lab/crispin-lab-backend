package com.crispinlab.space.domain.mention

import com.crispinlab.common.domain.EntityId

data class MentionId(
    override val value: Long
) : EntityId {
    companion object {
        fun String.asMentionId(): MentionId =
            MentionId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("멘션 ID 형식이 올바르지 않습니다.")
            )
    }
}

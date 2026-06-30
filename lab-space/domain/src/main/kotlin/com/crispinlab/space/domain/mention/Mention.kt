package com.crispinlab.space.domain.mention

import com.crispinlab.common.domain.Entity
import com.crispinlab.user.domain.user.UserId
import java.time.Instant
import java.time.Instant.now

class Mention(
    override val id: MentionId,
    val sourceType: SourceType,
    val sourceId: Long,
    val mentionedUserId: UserId,
    val mentionedByUserId: UserId,
    val createdAt: Instant = now()
) : Entity<MentionId> {
    enum class SourceType {
        PAGE,
        COMMENT
        ;

        companion object {
            fun String.asSourceType(): SourceType =
                entries.firstOrNull { it.name == uppercase() }
                    ?: throw IllegalArgumentException("지원하지 않는 mention source type 입니다.")
        }
    }
}

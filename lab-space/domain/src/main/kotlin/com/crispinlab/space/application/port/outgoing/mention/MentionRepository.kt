package com.crispinlab.space.application.port.outgoing.mention

import com.crispinlab.space.domain.mention.Mention

interface MentionRepository {
    fun replaceMentionsFor(
        sourceType: Mention.SourceType,
        sourceId: Long,
        mentions: List<Mention>
    ): List<Mention>

    fun findBy(
        sourceType: Mention.SourceType,
        sourceId: Long
    ): List<Mention>
}

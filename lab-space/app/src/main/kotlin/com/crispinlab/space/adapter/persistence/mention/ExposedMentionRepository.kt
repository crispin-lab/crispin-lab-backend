package com.crispinlab.space.adapter.persistence.mention

import com.crispinlab.space.application.port.outgoing.mention.MentionRepository
import com.crispinlab.space.domain.mention.Mention
import com.crispinlab.space.domain.mention.Mention.SourceType.Companion.asSourceType
import com.crispinlab.space.domain.mention.MentionId
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedMentionRepository : MentionRepository {
    override fun replaceMentionsFor(
        sourceType: Mention.SourceType,
        sourceId: Long,
        mentions: List<Mention>
    ): List<Mention> {
        val deduped: List<Mention> = mentions.distinctBy { it.mentionedUserId }
        val prior: Set<UserId> = findBy(sourceType, sourceId).map { it.mentionedUserId }.toSet()
        Mentions.deleteWhere {
            (Mentions.sourceType eq sourceType.name) and (Mentions.sourceId eq sourceId)
        }
        if (deduped.isNotEmpty()) {
            Mentions.batchInsert(deduped) { mention ->
                this[Mentions.id] = mention.id.value
                this[Mentions.sourceType] = mention.sourceType.name
                this[Mentions.sourceId] = mention.sourceId
                this[Mentions.mentionedUserId] = mention.mentionedUserId.value
                this[Mentions.mentionedByUserId] = mention.mentionedByUserId.value
                this[Mentions.createdAt] = mention.createdAt
            }
        }
        return deduped.filterNot { it.mentionedUserId in prior }
    }

    override fun findBy(
        sourceType: Mention.SourceType,
        sourceId: Long
    ): List<Mention> =
        Mentions
            .selectAll()
            .where {
                (Mentions.sourceType eq sourceType.name) and (Mentions.sourceId eq sourceId)
            }.map { it.toEntity() }

    private fun ResultRow.toEntity(): Mention =
        Mention(
            id = MentionId(this[Mentions.id]),
            sourceType = decodeSourceType(this[Mentions.sourceType]),
            sourceId = this[Mentions.sourceId],
            mentionedUserId = UserId(this[Mentions.mentionedUserId]),
            mentionedByUserId = UserId(this[Mentions.mentionedByUserId]),
            createdAt = this[Mentions.createdAt]
        )

    private fun decodeSourceType(stored: String): Mention.SourceType =
        runCatching { stored.asSourceType() }
            .getOrElse { cause ->
                throw IllegalStateException("저장된 mention source type 을 해석할 수 없습니다.", cause)
            }
}

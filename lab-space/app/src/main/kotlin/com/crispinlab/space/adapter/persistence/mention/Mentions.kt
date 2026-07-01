package com.crispinlab.space.adapter.persistence.mention

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object Mentions : Table("mentions") {
    val id = long("id")
    val sourceType = varchar("source_type", length = 20)
    val sourceId = long("source_id")
    val mentionedUserId = long("mentioned_user_id").index()
    val mentionedByUserId = long("mentioned_by_user_id")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("mentions_source_user_uidx", sourceType, sourceId, mentionedUserId)
    }
}

package com.crispinlab.notification.adapter.persistence.notification

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object Notifications : Table("notifications") {
    val id = long("id")
    val userId = long("user_id")
    val type = varchar("type", length = 20)
    val sourceType = varchar("source_type", length = 20)
    val sourceId = long("source_id")
    val actorUserId = long("actor_user_id")
    val isRead = bool("is_read")
    val readAt = timestamp("read_at").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index("notifications_user_created_at_idx", isUnique = false, userId, createdAt)
        uniqueIndex(
            "notifications_user_type_source_uidx",
            userId,
            type,
            sourceType,
            sourceId
        )
    }
}

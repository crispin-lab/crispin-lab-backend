package com.crispinlab.space.adapter.persistence.spacemember

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object SpaceMembers : Table("space_members") {
    val id = long("id")
    val spaceId = long("space_id").index("space_members_space_id_idx")
    val userId = long("user_id").index("space_members_user_id_idx")
    val role = varchar("role", length = 20)
    val joinedAt = timestamp("joined_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("space_members_space_id_user_id_uidx", spaceId, userId)
    }
}

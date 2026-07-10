package com.crispinlab.space.adapter.persistence.visit

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object SpaceVisits : Table("space_visits") {
    val id = long("id")
    val userId = long("user_id")
    val spaceId = long("space_id")
    val lastVisitedAt = timestamp("last_visited_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("space_visits_user_id_space_id_uidx", userId, spaceId)
    }
}

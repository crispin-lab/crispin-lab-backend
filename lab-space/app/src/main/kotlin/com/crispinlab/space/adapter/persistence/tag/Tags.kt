package com.crispinlab.space.adapter.persistence.tag

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object Tags : Table("tags") {
    val id = long("id")
    val spaceId = long("space_id").index("tags_space_id_idx")
    val name = varchar("name", length = 30)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("tags_space_id_name_uidx", spaceId, name)
    }
}

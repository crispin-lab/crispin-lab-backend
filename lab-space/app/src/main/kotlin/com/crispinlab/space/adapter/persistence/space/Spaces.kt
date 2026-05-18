package com.crispinlab.space.adapter.persistence.space

import com.crispinlab.space.domain.space.Space
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object Spaces : Table("spaces") {
    val id = long("id")
    val name = varchar("name", length = Space.MAX_NAME_LENGTH)
    val description = varchar("description", length = Space.MAX_DESCRIPTION_LENGTH)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

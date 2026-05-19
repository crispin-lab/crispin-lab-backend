package com.crispinlab.user.adapter.persistence.user

import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.Handle
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object Users : Table("users") {
    val id = long("id")
    val email = varchar("email", length = EmailAddress.MAX_LENGTH)
    val handle = varchar("handle", length = Handle.MAX_LENGTH)
    val role = varchar("role", length = 20)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

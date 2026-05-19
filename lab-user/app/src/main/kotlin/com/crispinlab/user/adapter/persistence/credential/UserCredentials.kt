package com.crispinlab.user.adapter.persistence.credential

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object UserCredentials : Table("user_credentials") {
    val id = long("id")
    val userId = long("user_id")
    val type = varchar("type", length = 20)
    val passwordHash = varchar("password_hash", length = 60).nullable()
    val oauthProvider = varchar("oauth_provider", length = 20).nullable()
    val oauthSubjectId = varchar("oauth_subject_id", length = 255).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

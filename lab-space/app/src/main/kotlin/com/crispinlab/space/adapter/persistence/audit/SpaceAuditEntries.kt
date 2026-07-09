package com.crispinlab.space.adapter.persistence.audit

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object SpaceAuditEntries : Table("space_audit_entries") {
    val id = long("id")
    val spaceId = long("space_id")
    val actorUserId = long("actor_user_id")
    val action = varchar("action", length = 20)
    val changeSummary = text("change_summary")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

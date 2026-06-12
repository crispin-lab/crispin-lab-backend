package com.crispinlab.space.adapter.persistence.page

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object PageLinks : Table("page_links") {
    val id = long("id")
    val pageId = long("page_id").index()
    val revisionId = long("revision_id").index()
    val targetPageId = long("target_page_id")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

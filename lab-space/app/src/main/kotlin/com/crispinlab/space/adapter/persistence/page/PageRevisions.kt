package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.domain.page.Page
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object PageRevisions : Table("page_revisions") {
    val id = long("id")
    val pageId = long("page_id")
    val version = integer("version")
    val title = varchar("title", length = Page.MAX_TITLE_LENGTH)
    val content = text("content")
    val authorId = long("author_id")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(pageId, version)
    }
}

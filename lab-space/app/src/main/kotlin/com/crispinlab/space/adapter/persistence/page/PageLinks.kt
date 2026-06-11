package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.domain.page.PageLink
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object PageLinks : Table("page_links") {
    val id = long("id")
    val pageId = long("page_id").index()
    val revisionId = long("revision_id").index()
    val targetPageId = long("target_page_id").nullable()
    val targetUrl = varchar("target_url", length = PageLink.MAX_EXTERNAL_URL_LENGTH).nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.domain.page.Page
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.javatime.timestamp

object Pages : Table("pages") {
    val id = long("id")
    val spaceId = long("space_id").index()
    val parentPageId = long("parent_page_id").nullable().index()
    val authorId = long("author_id")
    val title = varchar("title", length = Page.MAX_TITLE_LENGTH)
    val content = text("content")
    val visibility = varchar("visibility", length = 20)
    val currentVersion = integer("current_version")
    val displayOrder = integer("display_order")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)

    fun notDeleted(): Op<Boolean> = deletedAt.isNull()
}

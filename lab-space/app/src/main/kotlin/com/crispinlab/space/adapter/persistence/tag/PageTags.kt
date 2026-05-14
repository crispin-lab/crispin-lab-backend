package com.crispinlab.space.adapter.persistence.tag

import com.crispinlab.space.adapter.persistence.page.Pages
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object PageTags : Table("page_tags") {
    val pageId =
        long("page_id")
            .references(Pages.id, onDelete = ReferenceOption.CASCADE)
    val tagId =
        long("tag_id")
            .references(Tags.id, onDelete = ReferenceOption.CASCADE)
            .index("page_tags_tag_id_idx")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(pageId, tagId)
}

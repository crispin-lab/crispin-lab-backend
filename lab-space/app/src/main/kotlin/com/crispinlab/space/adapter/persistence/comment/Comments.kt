package com.crispinlab.space.adapter.persistence.comment

import com.crispinlab.space.domain.comment.Comment
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object Comments : Table("comments") {
    val id = long("id")
    val pageId = long("page_id").index("comments_page_id_idx")
    val authorId = long("author_id")
    val body = varchar("body", length = Comment.MAX_BODY_LENGTH)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

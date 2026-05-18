package com.crispinlab.space.adapter.persistence.comment

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.adapter.persistence.ExposedEntityRepository
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.user.UserId
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

@Repository
class ExposedCommentRepository :
    ExposedEntityRepository<Comment, CommentId>(),
    CommentRepository {
    override val table: Table = Comments
    override val idColumn: Column<Long> = Comments.id

    override fun ResultRow.toEntity(): Comment =
        Comment(
            id = CommentId(this[Comments.id]),
            pageId = PageId(this[Comments.pageId]),
            authorId = UserId(this[Comments.authorId]),
            body = this[Comments.body],
            createdAt = this[Comments.createdAt],
            updatedAt = this[Comments.updatedAt],
            deletedAt = this[Comments.deletedAt]
        )

    override fun insert(entity: Comment) {
        Comments.insert {
            it[id] = entity.id.value
            it[pageId] = entity.pageId.value
            it[authorId] = entity.authorId.value
            it[body] = entity.body
            it[createdAt] = entity.createdAt
            it[updatedAt] = entity.updatedAt
            it[deletedAt] = entity.deletedAt
        }
    }

    override fun update(entity: Comment) {
        Comments.update({ Comments.id eq entity.id.value }) {
            it[body] = entity.body
            it[updatedAt] = entity.updatedAt
            it[deletedAt] = entity.deletedAt
        }
    }

    override fun findByPageId(
        pageId: PageId,
        pageRequest: PageRequest
    ): PageResult<Comment> {
        val query = Comments.selectAll().where { Comments.pageId eq pageId.value }
        val totalElements: Long = query.count()
        val items: List<Comment> =
            query
                .orderBy(
                    Comments.createdAt to SortOrder.ASC,
                    Comments.id to SortOrder.ASC
                ).limit(pageRequest.size)
                .offset(pageRequest.offset)
                .map { it.toEntity() }
        return PageResult(
            items = items,
            page = pageRequest.page,
            size = pageRequest.size,
            totalElements = totalElements
        )
    }
}

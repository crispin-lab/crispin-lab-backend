package com.crispinlab.space.adapter.persistence.comment

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

@Repository
class ExposedCommentRepository : CommentRepository {
    override fun save(comment: Comment): Comment =
        Comments
            .selectAll()
            .where { Comments.id eq comment.id.value }
            .firstOrNull()
            ?.let { update(comment) }
            ?: insert(comment)

    override fun findBy(id: CommentId): Comment? =
        Comments
            .selectAll()
            .where { Comments.id eq id.value }
            .firstOrNull()
            ?.toEntity()

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

    override fun delete(id: CommentId) {
        Comments.deleteWhere { Comments.id eq id.value }
    }

    private fun insert(comment: Comment): Comment =
        comment.also {
            Comments.insert {
                it[id] = comment.id.value
                it[pageId] = comment.pageId.value
                it[authorId] = comment.authorId.value
                it[body] = comment.body
                it[createdAt] = comment.createdAt
                it[updatedAt] = comment.updatedAt
                it[deletedAt] = comment.deletedAt
            }
        }

    private fun update(comment: Comment): Comment =
        comment.also {
            Comments.update({ Comments.id eq comment.id.value }) {
                it[body] = comment.body
                it[updatedAt] = comment.updatedAt
                it[deletedAt] = comment.deletedAt
            }
        }

    private fun ResultRow.toEntity(): Comment =
        Comment(
            id = CommentId(this[Comments.id]),
            pageId = PageId(this[Comments.pageId]),
            authorId = UserId(this[Comments.authorId]),
            body = this[Comments.body],
            createdAt = this[Comments.createdAt],
            updatedAt = this[Comments.updatedAt],
            deletedAt = this[Comments.deletedAt]
        )
}

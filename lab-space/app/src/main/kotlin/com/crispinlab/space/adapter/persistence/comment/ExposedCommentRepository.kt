package com.crispinlab.space.adapter.persistence.comment

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.persistence.ExposedEntityRepository
import com.crispinlab.space.adapter.persistence.toPageResult
import com.crispinlab.space.application.port.outgoing.comment.CommentRepository
import com.crispinlab.space.domain.comment.Comment
import com.crispinlab.space.domain.comment.CommentContent
import com.crispinlab.space.domain.comment.CommentId
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedCommentRepository :
    ExposedEntityRepository<Comment, CommentId>(),
    CommentRepository {
    override val table = Comments
    override val idColumn = Comments.id
    override val deletedAtColumn = Comments.deletedAt
    override val updateExclude =
        listOf(
            Comments.id,
            Comments.pageId,
            Comments.authorId,
            Comments.createdAt,
            Comments.deletedAt
        )

    override fun ResultRow.toEntity(): Comment =
        Comment(
            id = CommentId(this[Comments.id]),
            pageId = PageId(this[Comments.pageId]),
            authorId = UserId(this[Comments.authorId]),
            content = CommentContent(this[Comments.content]),
            createdAt = this[Comments.createdAt],
            updatedAt = this[Comments.updatedAt],
            deletedAt = this[Comments.deletedAt]
        )

    @Suppress("RedundantOverride")
    override fun delete(id: CommentId) = super.delete(id)

    override fun upsertBody(
        builder: UpsertStatement<Long>,
        entity: Comment
    ) {
        builder[Comments.id] = entity.id.value
        builder[Comments.pageId] = entity.pageId.value
        builder[Comments.authorId] = entity.authorId.value
        builder[Comments.content] = entity.content.raw
        builder[Comments.createdAt] = entity.createdAt
        builder[Comments.updatedAt] = entity.updatedAt
        builder[Comments.deletedAt] = entity.deletedAt
    }

    override fun findByPageId(
        pageId: PageId,
        pageRequest: PageRequest
    ): PageResult<Comment> =
        Comments
            .selectAll()
            .where { (Comments.pageId eq pageId.value) and notDeleted() }
            .toPageResult(
                pageRequest,
                Comments.createdAt to SortOrder.ASC,
                Comments.id to SortOrder.ASC
            ) { it.toEntity() }
}

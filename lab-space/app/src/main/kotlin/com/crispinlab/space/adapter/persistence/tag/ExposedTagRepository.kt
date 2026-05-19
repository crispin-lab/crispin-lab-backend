package com.crispinlab.space.adapter.persistence.tag

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.adapter.persistence.ExposedEntityRepository
import com.crispinlab.space.adapter.persistence.page.Pages
import com.crispinlab.space.adapter.persistence.toPageResult
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.PageTag
import com.crispinlab.space.domain.tag.Tag
import com.crispinlab.space.domain.tag.TagErrorCode
import com.crispinlab.space.domain.tag.TagId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

private const val UNIQUE_VIOLATION_SQL_STATE = "23505"

@Repository
class ExposedTagRepository :
    ExposedEntityRepository<Tag, TagId>(),
    TagRepository {
    override val table = Tags
    override val idColumn = Tags.id
    override val deletedAtColumn = null
    override val updateExclude = listOf(Tags.id, Tags.spaceId, Tags.createdAt)

    override fun ResultRow.toEntity(): Tag =
        Tag(
            id = TagId(this[Tags.id]),
            spaceId = SpaceId(this[Tags.spaceId]),
            name = this[Tags.name],
            createdAt = this[Tags.createdAt]
        )

    @Suppress("RedundantOverride")
    override fun delete(id: TagId) = super.delete(id)

    override fun save(entity: Tag): Tag =
        try {
            super.save(entity)
        } catch (e: ExposedSQLException) {
            if (e.sqlState == UNIQUE_VIOLATION_SQL_STATE) {
                throw ConflictException(TagErrorCode.TAG_NAME_DUPLICATED, cause = e)
            }
            throw e
        }

    override fun upsertBody(
        builder: UpsertStatement<Long>,
        entity: Tag
    ) {
        builder[Tags.id] = entity.id.value
        builder[Tags.spaceId] = entity.spaceId.value
        builder[Tags.name] = entity.name
        builder[Tags.createdAt] = entity.createdAt
    }

    override fun findBySpaceId(
        spaceId: SpaceId,
        pageRequest: PageRequest
    ): PageResult<Tag> =
        Tags
            .selectAll()
            .where { Tags.spaceId eq spaceId.value }
            .toPageResult(
                pageRequest,
                Tags.createdAt to SortOrder.ASC,
                Tags.id to SortOrder.ASC
            ) { it.toEntity() }

    override fun existsByNameAndSpaceId(
        spaceId: SpaceId,
        name: String
    ): Boolean =
        Tags
            .select(Tags.id)
            .where { (Tags.spaceId eq spaceId.value) and (Tags.name eq name) }
            .limit(1)
            .empty()
            .not()

    override fun attach(pageTag: PageTag) {
        PageTags.insertIgnore {
            it[pageId] = pageTag.pageId.value
            it[tagId] = pageTag.tagId.value
            it[createdAt] = pageTag.createdAt
        }
    }

    override fun detach(
        pageId: PageId,
        tagId: TagId
    ) {
        PageTags.deleteWhere {
            (PageTags.pageId eq pageId.value) and (PageTags.tagId eq tagId.value)
        }
    }

    override fun findTagsByPageId(
        pageId: PageId,
        pageRequest: PageRequest
    ): PageResult<Tag> =
        (Tags innerJoin PageTags innerJoin Pages)
            .select(Tags.columns)
            .where { (PageTags.pageId eq pageId.value) and Pages.notDeleted() }
            .toPageResult(
                pageRequest,
                Tags.createdAt to SortOrder.ASC,
                Tags.id to SortOrder.ASC
            ) { it.toEntity() }

    override fun findPageIdsByTagId(tagId: TagId): List<PageId> =
        PageTags
            .select(PageTags.pageId)
            .where { PageTags.tagId eq tagId.value }
            .map { PageId(it[PageTags.pageId]) }
}

package com.crispinlab.space.adapter.persistence.tag

import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.PageTag
import com.crispinlab.space.domain.tag.Tag
import com.crispinlab.space.domain.tag.TagId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

@Repository
class ExposedTagRepository : TagRepository {
    override fun save(tag: Tag): Tag =
        Tags
            .selectAll()
            .where { Tags.id eq tag.id.value }
            .firstOrNull()
            ?.let { update(tag) }
            ?: insert(tag)

    override fun findBy(id: TagId): Tag? =
        Tags
            .selectAll()
            .where { Tags.id eq id.value }
            .firstOrNull()
            ?.toEntity()

    override fun findBySpaceId(spaceId: SpaceId): List<Tag> =
        Tags
            .selectAll()
            .where { Tags.spaceId eq spaceId.value }
            .map { it.toEntity() }

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

    override fun delete(id: TagId) {
        Tags.deleteWhere { Tags.id eq id.value }
    }

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

    override fun findTagsByPageId(pageId: PageId): List<Tag> =
        (Tags innerJoin PageTags)
            .select(Tags.columns)
            .where { PageTags.pageId eq pageId.value }
            .map { it.toEntity() }

    override fun findPageIdsByTagId(tagId: TagId): List<PageId> =
        PageTags
            .select(PageTags.pageId)
            .where { PageTags.tagId eq tagId.value }
            .map { PageId(it[PageTags.pageId]) }

    private fun insert(tag: Tag): Tag =
        tag.also {
            Tags.insert {
                it[id] = tag.id.value
                it[spaceId] = tag.spaceId.value
                it[name] = tag.name
                it[createdAt] = tag.createdAt
            }
        }

    private fun update(tag: Tag): Tag =
        tag.also {
            Tags.update({ Tags.id eq tag.id.value }) {
                it[name] = tag.name
            }
        }

    private fun ResultRow.toEntity(): Tag =
        Tag(
            id = TagId(this[Tags.id]),
            spaceId = SpaceId(this[Tags.spaceId]),
            name = this[Tags.name],
            createdAt = this[Tags.createdAt]
        )
}

package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.common.persistence.ExposedEntityRepository
import com.crispinlab.space.adapter.persistence.space.Spaces
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.page.PageVisibilityRecord
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.page.Visibility.Companion.asVisibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.space.SpaceVisibility.Companion.asSpaceVisibility
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class ExposedPageRepository :
    ExposedEntityRepository<Page, PageId>(),
    PageRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    override val table = Pages
    override val idColumn = Pages.id
    override val deletedAtColumn = Pages.deletedAt
    override val updateExclude =
        listOf(Pages.id, Pages.spaceId, Pages.authorId, Pages.createdAt, Pages.deletedAt)

    override fun ResultRow.toEntity(): Page =
        Page(
            id = PageId(this[Pages.id]),
            spaceId = SpaceId(this[Pages.spaceId]),
            parentPageId = this[Pages.parentPageId]?.let { PageId(it) },
            authorId = UserId(this[Pages.authorId]),
            title = this[Pages.title],
            content = PageContent(this[Pages.content]),
            visibility = decodeVisibility(this[Pages.visibility]),
            currentVersion = this[Pages.currentVersion],
            displayOrder = this[Pages.displayOrder],
            createdAt = this[Pages.createdAt],
            updatedAt = this[Pages.updatedAt],
            deletedAt = this[Pages.deletedAt]
        )

    @Suppress("RedundantOverride")
    override fun delete(id: PageId) = super.delete(id)

    override fun upsertBody(
        builder: UpsertStatement<Long>,
        entity: Page
    ) {
        builder[Pages.id] = entity.id.value
        builder[Pages.spaceId] = entity.spaceId.value
        builder[Pages.parentPageId] = entity.parentPageId?.value
        builder[Pages.authorId] = entity.authorId.value
        builder[Pages.title] = entity.title
        builder[Pages.content] = entity.content.raw
        builder[Pages.visibility] = entity.visibility.name
        builder[Pages.currentVersion] = entity.currentVersion
        builder[Pages.displayOrder] = entity.displayOrder
        builder[Pages.createdAt] = entity.createdAt
        builder[Pages.updatedAt] = entity.updatedAt
        builder[Pages.deletedAt] = entity.deletedAt
    }

    override fun findChildren(parentId: PageId): List<Page> =
        Pages
            .selectAll()
            .where { (Pages.parentPageId eq parentId.value) and notDeleted() }
            .map { it.toEntity() }

    override fun findVisibilitiesByIds(ids: Collection<PageId>): Map<PageId, PageVisibilityRecord> {
        if (ids.isEmpty()) return emptyMap()
        val rawIds: List<Long> = ids.map { it.value }
        return Pages
            .join(
                otherTable = Spaces,
                joinType = JoinType.INNER,
                additionalConstraint = { Pages.spaceId eq Spaces.id }
            ).select(
                Pages.id,
                Pages.visibility,
                Pages.spaceId,
                Pages.authorId,
                Spaces.visibility
            ).where {
                (Pages.id inList rawIds) and notDeleted() and Spaces.deletedAt.isNull()
            }.mapNotNull { row ->
                val pageId = PageId(row[Pages.id])
                val visibility =
                    decodeVisibilityOrSkip(row[Pages.visibility], pageId) ?: return@mapNotNull null
                val spaceVisibility =
                    decodeSpaceVisibilityOrSkip(row[Spaces.visibility], pageId)
                        ?: return@mapNotNull null
                pageId to
                    PageVisibilityRecord(
                        pageId = pageId,
                        visibility = visibility,
                        spaceId = SpaceId(row[Pages.spaceId]),
                        spaceVisibility = spaceVisibility,
                        authorId = UserId(row[Pages.authorId])
                    )
            }.toMap()
    }

    private fun decodeVisibilityOrSkip(
        stored: String,
        pageId: PageId
    ): Visibility? =
        runCatching { stored.asVisibility() }
            .onFailure {
                log.warn(
                    "저장된 page visibility 값을 해석할 수 없습니다 — skip. pageId={}",
                    pageId.value
                )
            }.getOrNull()

    private fun decodeSpaceVisibilityOrSkip(
        stored: String,
        pageId: PageId
    ): SpaceVisibility? =
        runCatching { stored.asSpaceVisibility() }
            .onFailure {
                log.warn(
                    "저장된 space visibility 값을 해석할 수 없습니다 — skip. pageId={}",
                    pageId.value
                )
            }.getOrNull()

    override fun findRoots(spaceId: SpaceId): List<Page> =
        Pages
            .selectAll()
            .where {
                (Pages.spaceId eq spaceId.value) and Pages.parentPageId.isNull() and
                    notDeleted()
            }.map { it.toEntity() }

    override fun nextDisplayOrderIn(
        spaceId: SpaceId,
        parentPageId: PageId?
    ): Int {
        val maxColumn = Pages.displayOrder.max()
        val parentCondition =
            parentPageId
                ?.let { Pages.parentPageId eq it.value }
                ?: Pages.parentPageId.isNull()
        val current =
            Pages
                .select(maxColumn)
                .where {
                    (Pages.spaceId eq spaceId.value) and parentCondition and notDeleted()
                }.single()[maxColumn]
        return (current ?: -1) + 1
    }
}

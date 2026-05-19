package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.adapter.persistence.ExposedEntityRepository
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.page.Visibility.Companion.asVisibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

@Repository
class ExposedPageRepository :
    ExposedEntityRepository<Page, PageId>(),
    PageRepository {
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
        builder[Pages.createdAt] = entity.createdAt
        builder[Pages.updatedAt] = entity.updatedAt
        builder[Pages.deletedAt] = entity.deletedAt
    }

    override fun findChildren(parentId: PageId): List<Page> =
        Pages
            .selectAll()
            .where { (Pages.parentPageId eq parentId.value) and notDeleted() }
            .map { it.toEntity() }

    override fun findRoots(spaceId: SpaceId): List<Page> =
        Pages
            .selectAll()
            .where {
                (Pages.spaceId eq spaceId.value) and Pages.parentPageId.isNull() and
                    notDeleted()
            }.map { it.toEntity() }

    private fun decodeVisibility(stored: String): Visibility =
        runCatching { stored.asVisibility() }
            .getOrElse { cause ->
                throw IllegalStateException("저장된 visibility 값을 해석할 수 없습니다.", cause)
            }
}

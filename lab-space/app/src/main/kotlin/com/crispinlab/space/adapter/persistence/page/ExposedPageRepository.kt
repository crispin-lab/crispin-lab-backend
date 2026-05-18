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
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

@Repository
class ExposedPageRepository :
    ExposedEntityRepository<Page, PageId>(),
    PageRepository {
    override val table = Pages
    override val idColumn = Pages.id
    override val deletedAtColumn = Pages.deletedAt

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

    override fun insert(entity: Page) {
        Pages.insert {
            it[id] = entity.id.value
            it[spaceId] = entity.spaceId.value
            it[parentPageId] = entity.parentPageId?.value
            it[authorId] = entity.authorId.value
            it[title] = entity.title
            it[content] = entity.content.raw
            it[visibility] = entity.visibility.name
            it[currentVersion] = entity.currentVersion
            it[createdAt] = entity.createdAt
            it[updatedAt] = entity.updatedAt
            it[deletedAt] = entity.deletedAt
        }
    }

    override fun update(entity: Page) {
        Pages.update({ Pages.id eq entity.id.value }) {
            it[parentPageId] = entity.parentPageId?.value
            it[title] = entity.title
            it[content] = entity.content.raw
            it[visibility] = entity.visibility.name
            it[currentVersion] = entity.currentVersion
            it[updatedAt] = entity.updatedAt
            it[deletedAt] = entity.deletedAt
        }
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

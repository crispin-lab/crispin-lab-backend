package com.crispinlab.space.adapter.persistence.page

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
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository

@Repository
class ExposedPageRepository : PageRepository {
    override fun save(page: Page): Page =
        Pages
            .selectAll()
            .where { Pages.id eq page.id.value }
            .firstOrNull()
            ?.let { update(page) }
            ?: insert(page)

    override fun findBy(id: PageId): Page? =
        Pages
            .selectAll()
            .where { Pages.id eq id.value }
            .firstOrNull()
            ?.toEntity()

    override fun findChildren(parentId: PageId): List<Page> =
        Pages
            .selectAll()
            .where { Pages.parentPageId eq parentId.value }
            .map { it.toEntity() }

    override fun findRoots(spaceId: SpaceId): List<Page> =
        Pages
            .selectAll()
            .where { (Pages.spaceId eq spaceId.value) and Pages.parentPageId.isNull() }
            .map { it.toEntity() }

    override fun delete(id: PageId) {
        Pages.deleteWhere { Pages.id eq id.value }
    }

    private fun insert(page: Page): Page =
        page.also {
            Pages.insert {
                it[id] = page.id.value
                it[spaceId] = page.spaceId.value
                it[parentPageId] = page.parentPageId?.value
                it[authorId] = page.authorId.value
                it[title] = page.title
                it[content] = page.content.raw
                it[visibility] = page.visibility.name
                it[currentVersion] = page.currentVersion
                it[createdAt] = page.createdAt
                it[updatedAt] = page.updatedAt
            }
        }

    private fun update(page: Page): Page =
        page.also {
            Pages.update({ Pages.id eq page.id.value }) {
                it[parentPageId] = page.parentPageId?.value
                it[title] = page.title
                it[content] = page.content.raw
                it[visibility] = page.visibility.name
                it[currentVersion] = page.currentVersion
                it[updatedAt] = page.updatedAt
            }
        }

    private fun decodeVisibility(stored: String): Visibility =
        runCatching { stored.asVisibility() }
            .getOrElse { cause ->
                throw IllegalStateException("저장된 visibility 값을 해석할 수 없습니다.", cause)
            }

    private fun ResultRow.toEntity(): Page =
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
            updatedAt = this[Pages.updatedAt]
        )
}

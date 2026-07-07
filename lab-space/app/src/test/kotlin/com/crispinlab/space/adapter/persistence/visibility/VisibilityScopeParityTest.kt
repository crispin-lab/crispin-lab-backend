package com.crispinlab.space.adapter.persistence.visibility

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.adapter.persistence.page.ExposedPageRepository
import com.crispinlab.space.adapter.persistence.tag.PageTags
import com.crispinlab.space.adapter.persistence.tag.Tags
import com.crispinlab.space.adapter.search.page.ExposedPageSearchAdapter
import com.crispinlab.space.adapter.search.tag.ExposedTagPopularitySearchAdapter
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.SortOption
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.seedSpaces
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class VisibilityScopeParityTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val pageRepository = ExposedPageRepository()

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("visibility 룰 직렬화기 parity") {
            it("모든 viewer × visibility × authorship 조합에서 DSL / raw SQL 결과가 동일하다") {
                val viewerId = UserId(100L)
                val otherId = UserId(200L)

                seedSpaces(
                    database,
                    PUBLIC_SPACE.value to SpaceVisibility.PUBLIC,
                    INTERNAL_SPACE.value to SpaceVisibility.INTERNAL
                )
                transaction(database) {
                    matrixPages(viewerId = viewerId, otherId = otherId)
                        .forEach { pageRepository.save(it) }
                    matrixRows.forEach { row ->
                        insertTag(
                            tagId = row.id,
                            spaceId = row.space.value,
                            name = "tag-${row.id}"
                        )
                        attachPageTag(pageId = row.id, tagId = row.id)
                    }
                }

                val scopes =
                    listOf(
                        "Anonymous" to VisibilityScope.Anonymous,
                        "Auth non-member" to
                            VisibilityScope.Authenticated(
                                viewerId = viewerId,
                                memberOfSpaceIds = emptySet()
                            ),
                        "Auth member of PUBLIC space" to
                            VisibilityScope.Authenticated(
                                viewerId = viewerId,
                                memberOfSpaceIds = setOf(PUBLIC_SPACE)
                            ),
                        "Auth member of INTERNAL space" to
                            VisibilityScope.Authenticated(
                                viewerId = viewerId,
                                memberOfSpaceIds = setOf(INTERNAL_SPACE)
                            ),
                        "Auth member of both spaces" to
                            VisibilityScope.Authenticated(
                                viewerId = viewerId,
                                memberOfSpaceIds = setOf(PUBLIC_SPACE, INTERNAL_SPACE)
                            ),
                        "Privileged" to VisibilityScope.Privileged
                    )

                scopes.forEach { (label, scope) ->
                    val expected =
                        expectedVisibleIds(scope = scope, viewerId = viewerId, otherId = otherId)
                    val dslIds = queryVisibleIdsViaDsl(scope)
                    val sqlIds = queryVisibleIdsViaRawSql(scope)

                    withClue("scope=$label — DSL 과 raw SQL 결과가 달라지면 두 직렬화기가 drift 한 것") {
                        dslIds shouldBe expected
                        sqlIds shouldBe expected
                    }
                }
            }
        }
    }) {
    companion object {
        private val MATRIX_PAGE_SIZE: Int get() = matrixRows.size

        private val PUBLIC_SPACE = SpaceId(10L)
        private val INTERNAL_SPACE = SpaceId(20L)

        private val pageAdapter = ExposedPageSearchAdapter()
        private val tagAdapter = ExposedTagPopularitySearchAdapter()

        private data class Row(
            val id: Long,
            val space: SpaceId,
            val visibility: Visibility,
            val authorIsViewer: Boolean
        )

        private val matrixRows: List<Row> =
            buildList {
                var nextId = 1L
                listOf(PUBLIC_SPACE, INTERNAL_SPACE).forEach { space ->
                    listOf(
                        Visibility.PUBLIC,
                        Visibility.MEMBER,
                        Visibility.INTERNAL,
                        Visibility.DRAFT
                    ).forEach { visibility ->
                        listOf(false, true).forEach { authorIsViewer ->
                            add(
                                Row(
                                    id = nextId++,
                                    space = space,
                                    visibility = visibility,
                                    authorIsViewer = authorIsViewer
                                )
                            )
                        }
                    }
                }
            }

        private fun spaceVisibilityOf(spaceId: SpaceId): SpaceVisibility =
            when (spaceId) {
                PUBLIC_SPACE -> SpaceVisibility.PUBLIC
                INTERNAL_SPACE -> SpaceVisibility.INTERNAL
                else -> error("unknown space $spaceId")
            }

        fun matrixPages(
            viewerId: UserId,
            otherId: UserId
        ): List<Page> =
            matrixRows.map { row ->
                basicPage(
                    id = PageId(row.id),
                    spaceId = row.space,
                    authorId = if (row.authorIsViewer) viewerId else otherId,
                    visibility = row.visibility
                )
            }

        fun expectedVisibleIds(
            scope: VisibilityScope,
            viewerId: UserId,
            otherId: UserId
        ): Set<Long> =
            matrixRows
                .filter { row ->
                    scope.allows(
                        pageVisibility = row.visibility,
                        spaceVisibility = spaceVisibilityOf(row.space),
                        spaceId = row.space,
                        authorId = if (row.authorIsViewer) viewerId else otherId
                    )
                }.map { it.id }
                .toSet()

        fun queryVisibleIdsViaDsl(scope: VisibilityScope): Set<Long> =
            transaction(PostgresTestContext.database) {
                pageAdapter
                    .search(
                        keyword = null,
                        spaceId = null,
                        tagIds = emptyList(),
                        tagIdsAnyOf = emptyList(),
                        parentPageId = null,
                        onlyRoot = false,
                        sort = SortOption.CREATED_AT,
                        scope = scope,
                        pageRequest = PageRequest(page = 0, size = MATRIX_PAGE_SIZE)
                    ).items
                    .map { it.id.value }
                    .toSet()
            }

        fun queryVisibleIdsViaRawSql(scope: VisibilityScope): Set<Long> =
            transaction(PostgresTestContext.database) {
                tagAdapter
                    .search(
                        scope = scope,
                        pageRequest = PageRequest(page = 0, size = MATRIX_PAGE_SIZE)
                    ).items
                    .map { it.name.removePrefix("tag-").toLong() }
                    .toSet()
            }

        fun insertTag(
            tagId: Long,
            spaceId: Long,
            name: String
        ) {
            Tags.insert {
                it[id] = tagId
                it[Tags.spaceId] = spaceId
                it[Tags.name] = name
                it[createdAt] = DUMMY_INSTANT
            }
        }

        fun attachPageTag(
            pageId: Long,
            tagId: Long
        ) {
            PageTags.insert {
                it[PageTags.pageId] = pageId
                it[PageTags.tagId] = tagId
                it[createdAt] = DUMMY_INSTANT
            }
        }
    }
}

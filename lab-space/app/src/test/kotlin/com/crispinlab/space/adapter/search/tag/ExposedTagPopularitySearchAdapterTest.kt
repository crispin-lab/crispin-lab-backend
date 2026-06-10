package com.crispinlab.space.adapter.search.tag

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.adapter.persistence.page.ExposedPageRepository
import com.crispinlab.space.adapter.persistence.tag.PageTags
import com.crispinlab.space.adapter.persistence.tag.Tags
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedTagPopularitySearchAdapterTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val pageRepository = ExposedPageRepository()
        val adapter = ExposedTagPopularitySearchAdapter()

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("인기 태그 검색") {
            it("count 내림차순, 동률은 name 오름차순으로 정렬한다") {
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L)))
                    pageRepository.save(publicPage(id = PageId(2L)))
                    pageRepository.save(publicPage(id = PageId(3L)))
                    insertTag(tagId = 1L, name = "kotlin")
                    insertTag(tagId = 2L, name = "spring")
                    insertTag(tagId = 3L, name = "exposed")
                    attachPageTag(pageId = 1L, tagId = 1L)
                    attachPageTag(pageId = 2L, tagId = 1L)
                    attachPageTag(pageId = 3L, tagId = 1L)
                    attachPageTag(pageId = 1L, tagId = 2L)
                    attachPageTag(pageId = 2L, tagId = 3L)
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.name } shouldBe listOf("kotlin", "exposed", "spring")
                result.items.map { it.usageCount } shouldBe listOf(3L, 1L, 1L)
                result.totalElements shouldBe 3L
            }

            it("같은 name 의 tag 가 여러 space 에 흩어져 있어도 cross-space 합산한다") {
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L), spaceId = SpaceId(10L)))
                    pageRepository.save(publicPage(id = PageId(2L), spaceId = SpaceId(20L)))
                    insertTag(tagId = 1L, spaceId = 10L, name = "kotlin")
                    insertTag(tagId = 2L, spaceId = 20L, name = "kotlin")
                    attachPageTag(pageId = 1L, tagId = 1L)
                    attachPageTag(pageId = 2L, tagId = 2L)
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items shouldHaveSize 1
                result.items.single().name shouldBe "kotlin"
                result.items.single().usageCount shouldBe 2L
                result.totalElements shouldBe 1L
            }

            it("Anonymous 는 PUBLIC 페이지의 태그만 집계한다") {
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L)))
                    pageRepository.save(
                        basicPage(id = PageId(2L), visibility = Visibility.INTERNAL)
                    )
                    pageRepository.save(
                        basicPage(id = PageId(3L), visibility = Visibility.DRAFT)
                    )
                    insertTag(tagId = 1L, name = "public-tag")
                    insertTag(tagId = 2L, name = "internal-tag")
                    insertTag(tagId = 3L, name = "draft-tag")
                    attachPageTag(pageId = 1L, tagId = 1L)
                    attachPageTag(pageId = 2L, tagId = 2L)
                    attachPageTag(pageId = 3L, tagId = 3L)
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.name } shouldBe listOf("public-tag")
                result.totalElements shouldBe 1L
            }

            it("Authenticated 는 PUBLIC + 본인 DRAFT + 멤버 스페이스의 INTERNAL 을 집계한다") {
                val viewerId = UserId(100L)
                val otherId = UserId(200L)
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L)))
                    pageRepository.save(
                        basicPage(
                            id = PageId(2L),
                            spaceId = SpaceId(10L),
                            visibility = Visibility.INTERNAL
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(3L),
                            spaceId = SpaceId(20L),
                            visibility = Visibility.INTERNAL
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(4L),
                            authorId = viewerId,
                            visibility = Visibility.DRAFT
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(5L),
                            authorId = otherId,
                            visibility = Visibility.DRAFT
                        )
                    )
                    insertTag(tagId = 1L, name = "public-tag")
                    insertTag(tagId = 2L, name = "member-internal")
                    insertTag(tagId = 3L, spaceId = 20L, name = "non-member-internal")
                    insertTag(tagId = 4L, name = "my-draft")
                    insertTag(tagId = 5L, name = "other-draft")
                    attachPageTag(pageId = 1L, tagId = 1L)
                    attachPageTag(pageId = 2L, tagId = 2L)
                    attachPageTag(pageId = 3L, tagId = 3L)
                    attachPageTag(pageId = 4L, tagId = 4L)
                    attachPageTag(pageId = 5L, tagId = 5L)
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            scope =
                                VisibilityScope.Authenticated(
                                    viewerId = viewerId,
                                    memberOfSpaceIds = setOf(SpaceId(10L))
                                ),
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.name } shouldBe
                    listOf("member-internal", "my-draft", "public-tag")
                result.totalElements shouldBe 3L
            }

            it("Privileged 는 모든 visibility 의 페이지 태그를 집계한다") {
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L)))
                    pageRepository.save(
                        basicPage(id = PageId(2L), visibility = Visibility.INTERNAL)
                    )
                    pageRepository.save(
                        basicPage(id = PageId(3L), visibility = Visibility.DRAFT)
                    )
                    insertTag(tagId = 1L, name = "a")
                    insertTag(tagId = 2L, name = "b")
                    insertTag(tagId = 3L, name = "c")
                    attachPageTag(pageId = 1L, tagId = 1L)
                    attachPageTag(pageId = 2L, tagId = 2L)
                    attachPageTag(pageId = 3L, tagId = 3L)
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            scope = VisibilityScope.Privileged,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.name } shouldBe listOf("a", "b", "c")
                result.totalElements shouldBe 3L
            }

            it("soft-deleted 페이지의 태그는 집계에서 제외된다") {
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L)))
                    pageRepository.save(
                        basicPage(
                            id = PageId(2L),
                            visibility = Visibility.PUBLIC,
                            deletedAt = DUMMY_INSTANT
                        )
                    )
                    insertTag(tagId = 1L, name = "alive")
                    insertTag(tagId = 2L, name = "gone")
                    attachPageTag(pageId = 1L, tagId = 1L)
                    attachPageTag(pageId = 2L, tagId = 2L)
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.name } shouldBe listOf("alive")
                result.totalElements shouldBe 1L
            }

            it("paging — 동일 totalElements 위에서 page 별로 다른 슬라이스를 반환한다") {
                val names = listOf("tag-a", "tag-b", "tag-c", "tag-d", "tag-e")
                transaction(database) {
                    names.forEachIndexed { idx, name ->
                        val pageId = (idx + 1).toLong()
                        pageRepository.save(publicPage(id = PageId(pageId)))
                        insertTag(tagId = pageId, name = name)
                        attachPageTag(pageId = pageId, tagId = pageId)
                    }
                }

                val first =
                    transaction(database) {
                        adapter.search(
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest(page = 0, size = 2)
                        )
                    }
                val second =
                    transaction(database) {
                        adapter.search(
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest(page = 1, size = 2)
                        )
                    }

                first.items.map { it.name } shouldBe listOf("tag-a", "tag-b")
                first.totalElements shouldBe 5L
                first.hasNext shouldBe true
                second.items.map { it.name } shouldBe listOf("tag-c", "tag-d")
                second.totalElements shouldBe 5L
            }

            it("mixed-scope — 같은 name 의 cross-space tag 가 visibility 별로 부분 제거되어 합산된다") {
                val viewerId = UserId(100L)
                val otherId = UserId(200L)
                transaction(database) {
                    pageRepository.save(
                        publicPage(id = PageId(1L), spaceId = SpaceId(10L))
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(2L),
                            spaceId = SpaceId(10L),
                            visibility = Visibility.INTERNAL
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(3L),
                            spaceId = SpaceId(20L),
                            visibility = Visibility.INTERNAL
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(4L),
                            spaceId = SpaceId(10L),
                            authorId = viewerId,
                            visibility = Visibility.DRAFT
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(5L),
                            spaceId = SpaceId(10L),
                            authorId = otherId,
                            visibility = Visibility.DRAFT
                        )
                    )
                    insertTag(tagId = 1L, spaceId = 10L, name = "kotlin")
                    insertTag(tagId = 2L, spaceId = 20L, name = "kotlin")
                    attachPageTag(pageId = 1L, tagId = 1L)
                    attachPageTag(pageId = 2L, tagId = 1L)
                    attachPageTag(pageId = 3L, tagId = 2L)
                    attachPageTag(pageId = 4L, tagId = 1L)
                    attachPageTag(pageId = 5L, tagId = 1L)
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            scope =
                                VisibilityScope.Authenticated(
                                    viewerId = viewerId,
                                    memberOfSpaceIds = setOf(SpaceId(10L))
                                ),
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.single().name shouldBe "kotlin"
                result.items.single().usageCount shouldBe 3L
                result.totalElements shouldBe 1L
            }

            it("매칭되는 태그가 없으면 빈 페이지를 반환한다") {
                val result =
                    transaction(database) {
                        adapter.search(
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items shouldBe emptyList()
                result.totalElements shouldBe 0L
            }
        }
    }) {
    companion object {
        fun publicPage(
            id: PageId,
            spaceId: SpaceId = SpaceId(10L),
            authorId: UserId = UserId(100L)
        ): Page =
            basicPage(
                id = id,
                spaceId = spaceId,
                authorId = authorId,
                visibility = Visibility.PUBLIC
            )

        fun insertTag(
            tagId: Long,
            spaceId: Long = 10L,
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

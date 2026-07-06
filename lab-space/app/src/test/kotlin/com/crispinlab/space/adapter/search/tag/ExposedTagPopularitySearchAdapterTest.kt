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
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.seedPublicSpaces
import com.crispinlab.space.testsupport.seedSpaces
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
                seedPublicSpaces(database, 10L)
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
                seedPublicSpaces(database, 10L, 20L)
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
                seedPublicSpaces(database, 10L)
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L)))
                    pageRepository.save(
                        basicPage(id = PageId(2L), visibility = Visibility.INTERNAL)
                    )
                    pageRepository.save(
                        basicPage(id = PageId(3L), visibility = Visibility.MEMBER)
                    )
                    pageRepository.save(
                        basicPage(id = PageId(4L), visibility = Visibility.DRAFT)
                    )
                    insertTag(tagId = 1L, name = "public-tag")
                    insertTag(tagId = 2L, name = "internal-tag")
                    insertTag(tagId = 3L, name = "member-tag")
                    insertTag(tagId = 4L, name = "draft-tag")
                    attachPageTag(pageId = 1L, tagId = 1L)
                    attachPageTag(pageId = 2L, tagId = 2L)
                    attachPageTag(pageId = 3L, tagId = 3L)
                    attachPageTag(pageId = 4L, tagId = 4L)
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

            it(
                "Authenticated 는 PUBLIC + 멤버 space MEMBER + 본인 INTERNAL/DRAFT 를 집계하고 " +
                    "타인 INTERNAL/DRAFT 와 비멤버 space MEMBER 는 제외한다"
            ) {
                val viewerId = UserId(100L)
                val otherId = UserId(200L)
                seedPublicSpaces(database, 10L, 20L)
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L)))
                    pageRepository.save(
                        basicPage(
                            id = PageId(2L),
                            spaceId = SpaceId(10L),
                            authorId = otherId,
                            visibility = Visibility.MEMBER
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(3L),
                            spaceId = SpaceId(20L),
                            authorId = otherId,
                            visibility = Visibility.MEMBER
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(4L),
                            authorId = viewerId,
                            visibility = Visibility.INTERNAL
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(5L),
                            authorId = otherId,
                            visibility = Visibility.INTERNAL
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(6L),
                            authorId = viewerId,
                            visibility = Visibility.DRAFT
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(7L),
                            authorId = otherId,
                            visibility = Visibility.DRAFT
                        )
                    )
                    insertTag(tagId = 1L, name = "public-tag")
                    insertTag(tagId = 2L, name = "member-tag")
                    insertTag(tagId = 3L, spaceId = 20L, name = "non-member-tag")
                    insertTag(tagId = 4L, name = "my-internal")
                    insertTag(tagId = 5L, name = "other-internal")
                    insertTag(tagId = 6L, name = "my-draft")
                    insertTag(tagId = 7L, name = "other-draft")
                    attachPageTag(pageId = 1L, tagId = 1L)
                    attachPageTag(pageId = 2L, tagId = 2L)
                    attachPageTag(pageId = 3L, tagId = 3L)
                    attachPageTag(pageId = 4L, tagId = 4L)
                    attachPageTag(pageId = 5L, tagId = 5L)
                    attachPageTag(pageId = 6L, tagId = 6L)
                    attachPageTag(pageId = 7L, tagId = 7L)
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
                    listOf("member-tag", "my-draft", "my-internal", "public-tag")
                result.totalElements shouldBe 4L
            }

            it("Privileged 는 모든 visibility 의 페이지 태그를 집계한다") {
                seedPublicSpaces(database, 10L)
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L)))
                    pageRepository.save(
                        basicPage(id = PageId(2L), visibility = Visibility.INTERNAL)
                    )
                    pageRepository.save(
                        basicPage(id = PageId(3L), visibility = Visibility.MEMBER)
                    )
                    pageRepository.save(
                        basicPage(id = PageId(4L), visibility = Visibility.DRAFT)
                    )
                    insertTag(tagId = 1L, name = "a")
                    insertTag(tagId = 2L, name = "b")
                    insertTag(tagId = 3L, name = "c")
                    insertTag(tagId = 4L, name = "d")
                    attachPageTag(pageId = 1L, tagId = 1L)
                    attachPageTag(pageId = 2L, tagId = 2L)
                    attachPageTag(pageId = 3L, tagId = 3L)
                    attachPageTag(pageId = 4L, tagId = 4L)
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            scope = VisibilityScope.Privileged,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.name } shouldBe listOf("a", "b", "c", "d")
                result.totalElements shouldBe 4L
            }

            it("soft-deleted 페이지의 태그는 집계에서 제외된다") {
                seedPublicSpaces(database, 10L)
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
                seedPublicSpaces(database, 10L)
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
                seedPublicSpaces(database, 10L, 20L)
                transaction(database) {
                    pageRepository.save(
                        publicPage(id = PageId(1L), spaceId = SpaceId(10L))
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(2L),
                            spaceId = SpaceId(10L),
                            authorId = otherId,
                            visibility = Visibility.MEMBER
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(3L),
                            spaceId = SpaceId(20L),
                            authorId = otherId,
                            visibility = Visibility.MEMBER
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

            it("Anonymous 는 INTERNAL space 의 PUBLIC 페이지 태그를 노출하지 않는다") {
                seedSpaces(
                    database,
                    10L to SpaceVisibility.PUBLIC,
                    20L to SpaceVisibility.INTERNAL
                )
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L), spaceId = SpaceId(10L)))
                    pageRepository.save(publicPage(id = PageId(2L), spaceId = SpaceId(20L)))
                    insertTag(tagId = 1L, name = "public-tag")
                    insertTag(tagId = 2L, spaceId = 20L, name = "leak-tag")
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

                result.items.map { it.name } shouldBe listOf("public-tag")
                result.totalElements shouldBe 1L
            }

            it(
                "Authenticated 비멤버는 INTERNAL space 의 " +
                    "PUBLIC/MEMBER 페이지 태그를 노출하지 않는다"
            ) {
                val viewerId = UserId(100L)
                val otherId = UserId(200L)
                seedSpaces(
                    database,
                    10L to SpaceVisibility.PUBLIC,
                    20L to SpaceVisibility.INTERNAL
                )
                transaction(database) {
                    pageRepository.save(
                        publicPage(
                            id = PageId(1L),
                            spaceId = SpaceId(10L),
                            authorId = otherId
                        )
                    )
                    pageRepository.save(
                        publicPage(
                            id = PageId(2L),
                            spaceId = SpaceId(20L),
                            authorId = otherId
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(3L),
                            spaceId = SpaceId(20L),
                            authorId = otherId,
                            visibility = Visibility.MEMBER
                        )
                    )
                    insertTag(tagId = 1L, name = "public-tag")
                    insertTag(tagId = 2L, spaceId = 20L, name = "leak-public")
                    insertTag(tagId = 3L, spaceId = 20L, name = "leak-member")
                    attachPageTag(pageId = 1L, tagId = 1L)
                    attachPageTag(pageId = 2L, tagId = 2L)
                    attachPageTag(pageId = 3L, tagId = 3L)
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            scope =
                                VisibilityScope.Authenticated(
                                    viewerId = viewerId,
                                    memberOfSpaceIds = emptySet()
                                ),
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.name } shouldBe listOf("public-tag")
                result.totalElements shouldBe 1L
            }

            it(
                "Authenticated author 겸 스페이스 멤버는 자기 INTERNAL space 의 " +
                    "PUBLIC/MEMBER/INTERNAL 페이지 태그를 본다"
            ) {
                val viewerId = UserId(100L)
                val otherId = UserId(200L)
                seedSpaces(
                    database,
                    10L to SpaceVisibility.PUBLIC,
                    20L to SpaceVisibility.INTERNAL
                )
                transaction(database) {
                    pageRepository.save(
                        publicPage(
                            id = PageId(1L),
                            spaceId = SpaceId(10L),
                            authorId = otherId
                        )
                    )
                    pageRepository.save(
                        publicPage(
                            id = PageId(2L),
                            spaceId = SpaceId(20L),
                            authorId = viewerId
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(3L),
                            spaceId = SpaceId(20L),
                            authorId = viewerId,
                            visibility = Visibility.MEMBER
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(4L),
                            spaceId = SpaceId(20L),
                            authorId = viewerId,
                            visibility = Visibility.INTERNAL
                        )
                    )
                    insertTag(tagId = 1L, name = "public-tag")
                    insertTag(tagId = 2L, spaceId = 20L, name = "my-internal-public")
                    insertTag(tagId = 3L, spaceId = 20L, name = "my-internal-member")
                    insertTag(tagId = 4L, spaceId = 20L, name = "my-internal")
                    attachPageTag(pageId = 1L, tagId = 1L)
                    attachPageTag(pageId = 2L, tagId = 2L)
                    attachPageTag(pageId = 3L, tagId = 3L)
                    attachPageTag(pageId = 4L, tagId = 4L)
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            scope =
                                VisibilityScope.Authenticated(
                                    viewerId = viewerId,
                                    memberOfSpaceIds = setOf(SpaceId(20L))
                                ),
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.name } shouldBe
                    listOf(
                        "my-internal",
                        "my-internal-member",
                        "my-internal-public",
                        "public-tag"
                    )
                result.totalElements shouldBe 4L
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

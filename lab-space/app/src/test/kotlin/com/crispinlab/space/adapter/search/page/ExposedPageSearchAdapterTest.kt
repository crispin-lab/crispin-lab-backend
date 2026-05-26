package com.crispinlab.space.adapter.search.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.adapter.persistence.page.ExposedPageRepository
import com.crispinlab.space.adapter.persistence.tag.PageTags
import com.crispinlab.space.adapter.persistence.tag.Tags
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedPageSearchAdapterTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val pageRepository = ExposedPageRepository()
        val adapter = ExposedPageSearchAdapter()

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("페이지 검색") {
            it("필터가 없으면 발행된 페이지를 updatedAt 내림차순으로 반환한다") {
                transaction(database) {
                    pageRepository.save(
                        publicPage(
                            id = PageId(1L),
                            spaceId = SpaceId(10L),
                            title = "이전",
                            createdAt = DUMMY_INSTANT
                        )
                    )
                    pageRepository.save(
                        publicPage(
                            id = PageId(2L),
                            spaceId = SpaceId(10L),
                            title = "최근",
                            createdAt = DUMMY_INSTANT.plusSeconds(60)
                        )
                    )
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            keyword = null,
                            spaceId = null,
                            tagIds = emptyList(),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.id } shouldBe listOf(PageId(2L), PageId(1L))
                result.totalElements shouldBe 2L
            }

            it("키워드는 title 과 content 를 모두 LIKE 매칭한다") {
                transaction(database) {
                    pageRepository.save(
                        publicPage(id = PageId(1L), title = "회고 모음", content = PageContent("..."))
                    )
                    pageRepository.save(
                        publicPage(
                            id = PageId(2L),
                            title = "기타",
                            content = PageContent("오늘 회고를 작성")
                        )
                    )
                    pageRepository.save(
                        publicPage(id = PageId(3L), title = "무관", content = PageContent("무관"))
                    )
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            keyword = "회고",
                            spaceId = null,
                            tagIds = emptyList(),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.id }.toSet() shouldBe setOf(PageId(1L), PageId(2L))
                result.totalElements shouldBe 2L
            }

            it("키워드의 LIKE wildcard 문자는 literal 로 처리된다") {
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L), title = "할인율 90% 적용"))
                    pageRepository.save(publicPage(id = PageId(2L), title = "그 외 매칭되지 않음"))
                    pageRepository.save(publicPage(id = PageId(3L), title = "snake_case 이름"))
                }

                val percentResult =
                    transaction(database) {
                        adapter.search(
                            keyword = "90%",
                            spaceId = null,
                            tagIds = emptyList(),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }
                percentResult.items.map { it.id } shouldBe listOf(PageId(1L))

                val underscoreResult =
                    transaction(database) {
                        adapter.search(
                            keyword = "snake_case",
                            spaceId = null,
                            tagIds = emptyList(),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }
                underscoreResult.items.map { it.id } shouldBe listOf(PageId(3L))
            }

            it("발행되지 않은(DRAFT) 페이지는 검색 결과에서 제외된다") {
                transaction(database) {
                    pageRepository.save(
                        publicPage(id = PageId(1L), title = "공개된 회고")
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(2L),
                            title = "비공개 회고",
                            visibility = Visibility.DRAFT
                        )
                    )
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            keyword = "회고",
                            spaceId = null,
                            tagIds = emptyList(),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.id } shouldBe listOf(PageId(1L))
                result.totalElements shouldBe 1L
            }

            it("spaceId 필터는 해당 스페이스의 페이지만 반환한다") {
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L), spaceId = SpaceId(10L)))
                    pageRepository.save(publicPage(id = PageId(2L), spaceId = SpaceId(20L)))
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            keyword = null,
                            spaceId = SpaceId(20L),
                            tagIds = emptyList(),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items shouldHaveSize 1
                result.items.first().id shouldBe PageId(2L)
            }

            it("tagIds 필터는 그 태그가 붙은 페이지만 반환한다") {
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L)))
                    pageRepository.save(publicPage(id = PageId(2L)))
                    pageRepository.save(publicPage(id = PageId(3L)))
                    attachTag(pageId = 1L, tagId = 100L)
                    attachTag(pageId = 2L, tagId = 200L)
                    attachTag(pageId = 3L, tagId = 300L)
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            keyword = null,
                            spaceId = null,
                            tagIds = listOf(TagId(100L), TagId(200L)),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.id }.toSet() shouldBe setOf(PageId(1L), PageId(2L))
                result.totalElements shouldBe 2L
            }

            it("한 페이지가 검색에 들어가는 여러 태그를 모두 가져도 결과에 한 번만 나온다") {
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L)))
                    attachTag(pageId = 1L, tagId = 100L)
                    attachTag(pageId = 1L, tagId = 200L)
                    attachTag(pageId = 1L, tagId = 300L)
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            keyword = null,
                            spaceId = null,
                            tagIds = listOf(TagId(100L), TagId(200L), TagId(300L)),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.id } shouldBe listOf(PageId(1L))
                result.totalElements shouldBe 1L
            }

            it("매칭되는 태그가 하나도 없으면 빈 결과를 반환한다") {
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L)))
                    attachTag(pageId = 1L, tagId = 100L)
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            keyword = null,
                            spaceId = null,
                            tagIds = listOf(TagId(999L)),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items shouldBe emptyList()
                result.totalElements shouldBe 0L
            }

            it("keyword + spaceId + tagIds 조합을 동시에 적용한다") {
                transaction(database) {
                    pageRepository.save(
                        publicPage(id = PageId(1L), spaceId = SpaceId(10L), title = "회고")
                    )
                    pageRepository.save(
                        publicPage(id = PageId(2L), spaceId = SpaceId(20L), title = "회고")
                    )
                    pageRepository.save(
                        publicPage(id = PageId(3L), spaceId = SpaceId(10L), title = "무관")
                    )
                    attachTag(pageId = 1L, tagId = 100L)
                    attachTag(pageId = 2L, tagId = 100L)
                    attachTag(pageId = 3L, tagId = 100L)
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            keyword = "회고",
                            spaceId = SpaceId(10L),
                            tagIds = listOf(TagId(100L)),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.id } shouldBe listOf(PageId(1L))
            }

            it("page/size 로 결과를 슬라이스한다") {
                transaction(database) {
                    (1L..5L).forEach { id ->
                        pageRepository.save(
                            publicPage(
                                id = PageId(id),
                                createdAt = DUMMY_INSTANT.plusSeconds(id)
                            )
                        )
                    }
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            keyword = null,
                            spaceId = null,
                            tagIds = emptyList(),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest(page = 1, size = 2)
                        )
                    }

                result.items.map { it.id } shouldBe listOf(PageId(3L), PageId(2L))
                result.totalElements shouldBe 5L
                result.totalPages shouldBe 3
            }

            it("soft deleted 페이지는 기본 검색 결과에서 제외된다") {
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L), title = "살아있는 페이지"))
                    pageRepository.save(publicPage(id = PageId(2L), title = "삭제될 페이지"))
                }

                transaction(database) {
                    pageRepository.delete(PageId(2L))
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            keyword = null,
                            spaceId = null,
                            tagIds = emptyList(),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.id } shouldBe listOf(PageId(1L))
                result.totalElements shouldBe 1L
            }

            it("tag 매칭이 있어도 페이지가 soft delete 되었다면 결과에서 제외된다") {
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L), title = "살아있는 페이지"))
                    pageRepository.save(publicPage(id = PageId(2L), title = "삭제될 페이지"))
                    attachTag(pageId = 1L, tagId = 100L)
                    attachTag(pageId = 2L, tagId = 100L)
                }

                transaction(database) {
                    pageRepository.delete(PageId(2L))
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            keyword = null,
                            spaceId = null,
                            tagIds = listOf(TagId(100L)),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.id } shouldBe listOf(PageId(1L))
                result.totalElements shouldBe 1L
            }

            it("Authenticated scope 는 PUBLIC + INTERNAL + 본인 DRAFT 를 노출한다") {
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L), title = "공개"))
                    pageRepository.save(
                        basicPage(
                            id = PageId(2L),
                            title = "내부",
                            visibility = Visibility.INTERNAL
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(3L),
                            authorId = UserId(100L),
                            title = "본인 초안",
                            visibility = Visibility.DRAFT
                        )
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(4L),
                            authorId = UserId(200L),
                            title = "타인 초안",
                            visibility = Visibility.DRAFT
                        )
                    )
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            keyword = null,
                            spaceId = null,
                            tagIds = emptyList(),
                            scope =
                                VisibilityScope.Authenticated(
                                    viewerId = UserId(100L),
                                    memberOfSpaceIds = setOf(SpaceId(10L))
                                ),
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.id }.toSet() shouldBe
                    setOf(PageId(1L), PageId(2L), PageId(3L))
                result.totalElements shouldBe 3L
            }

            it("Privileged scope 는 모든 visibility 의 페이지를 노출한다") {
                transaction(database) {
                    pageRepository.save(publicPage(id = PageId(1L)))
                    pageRepository.save(
                        basicPage(id = PageId(2L), visibility = Visibility.INTERNAL)
                    )
                    pageRepository.save(
                        basicPage(
                            id = PageId(3L),
                            authorId = UserId(200L),
                            visibility = Visibility.DRAFT
                        )
                    )
                }

                val result =
                    transaction(database) {
                        adapter.search(
                            keyword = null,
                            spaceId = null,
                            tagIds = emptyList(),
                            scope = VisibilityScope.Privileged,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.id }.toSet() shouldBe
                    setOf(PageId(1L), PageId(2L), PageId(3L))
                result.totalElements shouldBe 3L
            }
        }
    }) {
    companion object {
        fun publicPage(
            id: PageId,
            spaceId: SpaceId = SpaceId(10L),
            title: String = "초안",
            content: PageContent = PageContent("본문"),
            createdAt: Instant = DUMMY_INSTANT
        ): Page =
            basicPage(
                id = id,
                spaceId = spaceId,
                title = title,
                content = content,
                visibility = Visibility.PUBLIC,
                createdAt = createdAt
            )

        fun attachTag(
            pageId: Long,
            tagId: Long,
            spaceId: Long = 10L
        ) {
            Tags.insertIgnore {
                it[id] = tagId
                it[Tags.spaceId] = spaceId
                it[name] = "tag-$tagId"
                it[createdAt] = DUMMY_INSTANT
            }
            PageTags.insert {
                it[PageTags.pageId] = pageId
                it[PageTags.tagId] = tagId
                it[createdAt] = DUMMY_INSTANT
            }
        }
    }
}

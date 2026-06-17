package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageLinkId
import com.crispinlab.space.domain.page.PageRevisionId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.Fixtures.basicPageLink
import com.crispinlab.space.testsupport.Fixtures.basicPageRevision
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedPageInboundLinkAdapterTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val pageRepository = ExposedPageRepository()
        val revisionRepository = ExposedPageRevisionRepository()
        val linkRepository = ExposedPageLinkRepository()
        val adapter = ExposedPageInboundLinkAdapter()

        afterEach { PostgresTestContext.truncateAll() }

        describe("ExposedPageInboundLinkAdapter") {
            it("Anonymous 는 PUBLIC source 만 응답으로 받는다") {
                transaction(database) {
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 11L,
                        sourceRevisionId = 101L,
                        sourceVisibility = Visibility.PUBLIC,
                        sourceSpaceId = 10L,
                        targetPageId = 50L,
                        linkId = 1001L
                    )
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 12L,
                        sourceRevisionId = 102L,
                        sourceVisibility = Visibility.INTERNAL,
                        sourceSpaceId = 10L,
                        targetPageId = 50L,
                        linkId = 1002L
                    )
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 13L,
                        sourceRevisionId = 103L,
                        sourceVisibility = Visibility.DRAFT,
                        sourceSpaceId = 10L,
                        sourceAuthorId = 200L,
                        targetPageId = 50L,
                        linkId = 1003L
                    )
                }

                val result =
                    transaction(database) {
                        adapter.findInboundLinksOf(
                            targetPageId = PageId(50L),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.pageId } shouldBe listOf(PageId(11L))
                result.totalElements shouldBe 1L
            }

            it("Authenticated 는 PUBLIC + 자기 멤버 INTERNAL + 자기 DRAFT 만 받는다") {
                transaction(database) {
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 11L,
                        sourceRevisionId = 101L,
                        sourceVisibility = Visibility.PUBLIC,
                        sourceSpaceId = 10L,
                        targetPageId = 50L,
                        linkId = 1001L
                    )
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 12L,
                        sourceRevisionId = 102L,
                        sourceVisibility = Visibility.INTERNAL,
                        sourceSpaceId = 10L,
                        targetPageId = 50L,
                        linkId = 1002L
                    )
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 13L,
                        sourceRevisionId = 103L,
                        sourceVisibility = Visibility.INTERNAL,
                        sourceSpaceId = 99L,
                        targetPageId = 50L,
                        linkId = 1003L
                    )
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 14L,
                        sourceRevisionId = 104L,
                        sourceVisibility = Visibility.DRAFT,
                        sourceSpaceId = 10L,
                        sourceAuthorId = 100L,
                        targetPageId = 50L,
                        linkId = 1004L
                    )
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 15L,
                        sourceRevisionId = 105L,
                        sourceVisibility = Visibility.DRAFT,
                        sourceSpaceId = 10L,
                        sourceAuthorId = 999L,
                        targetPageId = 50L,
                        linkId = 1005L
                    )
                }

                val result =
                    transaction(database) {
                        adapter.findInboundLinksOf(
                            targetPageId = PageId(50L),
                            scope =
                                VisibilityScope.Authenticated(
                                    viewerId = UserId(100L),
                                    memberOfSpaceIds = setOf(SpaceId(10L))
                                ),
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.pageId }.toSet() shouldBe
                    setOf(PageId(11L), PageId(12L), PageId(14L))
                result.totalElements shouldBe 3L
            }

            it("Privileged 는 모든 source 를 받는다") {
                transaction(database) {
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 11L,
                        sourceRevisionId = 101L,
                        sourceVisibility = Visibility.PUBLIC,
                        sourceSpaceId = 10L,
                        targetPageId = 50L,
                        linkId = 1001L
                    )
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 12L,
                        sourceRevisionId = 102L,
                        sourceVisibility = Visibility.DRAFT,
                        sourceSpaceId = 99L,
                        sourceAuthorId = 999L,
                        targetPageId = 50L,
                        linkId = 1002L
                    )
                }

                val result =
                    transaction(database) {
                        adapter.findInboundLinksOf(
                            targetPageId = PageId(50L),
                            scope = VisibilityScope.Privileged,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.pageId }.toSet() shouldBe setOf(PageId(11L), PageId(12L))
                result.totalElements shouldBe 2L
            }

            it("source 의 현재 revision 에 없는 historical 링크는 응답에서 제외된다") {
                transaction(database) {
                    pageRepository.save(
                        basicPage(
                            id = PageId(11L),
                            spaceId = SpaceId(10L),
                            visibility = Visibility.PUBLIC,
                            currentVersion = 2
                        )
                    )
                    revisionRepository.save(
                        basicPageRevision(
                            id = PageRevisionId(101L),
                            pageId = PageId(11L),
                            version = 1
                        )
                    )
                    revisionRepository.save(
                        basicPageRevision(
                            id = PageRevisionId(102L),
                            pageId = PageId(11L),
                            version = 2
                        )
                    )
                    linkRepository.saveAll(
                        listOf(
                            basicPageLink(
                                id = PageLinkId(1001L),
                                pageId = PageId(11L),
                                revisionId = PageRevisionId(101L),
                                target = PageId(50L)
                            )
                        )
                    )
                }

                val result =
                    transaction(database) {
                        adapter.findInboundLinksOf(
                            targetPageId = PageId(50L),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items shouldHaveSize 0
                result.totalElements shouldBe 0L
            }

            it("soft-deleted source 는 응답에서 제외된다") {
                transaction(database) {
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 11L,
                        sourceRevisionId = 101L,
                        sourceVisibility = Visibility.PUBLIC,
                        sourceSpaceId = 10L,
                        targetPageId = 50L,
                        linkId = 1001L
                    )
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 12L,
                        sourceRevisionId = 102L,
                        sourceVisibility = Visibility.PUBLIC,
                        sourceSpaceId = 10L,
                        targetPageId = 50L,
                        linkId = 1002L,
                        deletedAt = DUMMY_INSTANT.plusSeconds(60)
                    )
                }

                val result =
                    transaction(database) {
                        adapter.findInboundLinksOf(
                            targetPageId = PageId(50L),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.pageId } shouldBe listOf(PageId(11L))
                result.totalElements shouldBe 1L
            }

            it("updatedAt 내림차순으로 정렬한다") {
                transaction(database) {
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 11L,
                        sourceRevisionId = 101L,
                        sourceVisibility = Visibility.PUBLIC,
                        sourceSpaceId = 10L,
                        targetPageId = 50L,
                        linkId = 1001L,
                        sourceUpdatedAt = DUMMY_INSTANT
                    )
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 12L,
                        sourceRevisionId = 102L,
                        sourceVisibility = Visibility.PUBLIC,
                        sourceSpaceId = 10L,
                        targetPageId = 50L,
                        linkId = 1002L,
                        sourceUpdatedAt = DUMMY_INSTANT.plusSeconds(120)
                    )
                    seedSource(
                        pageRepository,
                        revisionRepository,
                        linkRepository,
                        sourcePageId = 13L,
                        sourceRevisionId = 103L,
                        sourceVisibility = Visibility.PUBLIC,
                        sourceSpaceId = 10L,
                        targetPageId = 50L,
                        linkId = 1003L,
                        sourceUpdatedAt = DUMMY_INSTANT.plusSeconds(60)
                    )
                }

                val result =
                    transaction(database) {
                        adapter.findInboundLinksOf(
                            targetPageId = PageId(50L),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items.map { it.pageId } shouldBe
                    listOf(PageId(12L), PageId(13L), PageId(11L))
            }

            it("페이징 — totalElements 와 hasNext 가 정확히 계산된다") {
                transaction(database) {
                    (1..4).forEach { idx ->
                        seedSource(
                            pageRepository,
                            revisionRepository,
                            linkRepository,
                            sourcePageId = (10L + idx),
                            sourceRevisionId = (100L + idx),
                            sourceVisibility = Visibility.PUBLIC,
                            sourceSpaceId = 10L,
                            targetPageId = 50L,
                            linkId = (1000L + idx),
                            sourceUpdatedAt = DUMMY_INSTANT.plusSeconds(idx.toLong())
                        )
                    }
                }

                val result =
                    transaction(database) {
                        adapter.findInboundLinksOf(
                            targetPageId = PageId(50L),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest(page = 0, size = 2)
                        )
                    }

                result.items shouldHaveSize 2
                result.totalElements shouldBe 4L
                result.hasNext shouldBe true
            }

            it("같은 source 가 본문에서 target 을 여러 번 참조해도 응답에는 한 번만 나온다") {
                transaction(database) {
                    pageRepository.save(
                        basicPage(
                            id = PageId(11L),
                            spaceId = SpaceId(10L),
                            visibility = Visibility.PUBLIC,
                            currentVersion = 1
                        )
                    )
                    revisionRepository.save(
                        basicPageRevision(
                            id = PageRevisionId(101L),
                            pageId = PageId(11L),
                            version = 1
                        )
                    )
                    linkRepository.saveAll(
                        listOf(
                            basicPageLink(
                                id = PageLinkId(1001L),
                                pageId = PageId(11L),
                                revisionId = PageRevisionId(101L),
                                target = PageId(50L)
                            ),
                            basicPageLink(
                                id = PageLinkId(1002L),
                                pageId = PageId(11L),
                                revisionId = PageRevisionId(101L),
                                target = PageId(50L)
                            )
                        )
                    )
                }

                val result =
                    transaction(database) {
                        adapter.findInboundLinksOf(
                            targetPageId = PageId(50L),
                            scope = VisibilityScope.Anonymous,
                            pageRequest = PageRequest.firstPage()
                        )
                    }

                result.items shouldHaveSize 1
                result.items.map { it.pageId } shouldBe listOf(PageId(11L))
                result.totalElements shouldBe 1L
            }
        }
    }) {
    companion object {
        @Suppress("LongParameterList")
        fun seedSource(
            pageRepository: ExposedPageRepository,
            revisionRepository: ExposedPageRevisionRepository,
            linkRepository: ExposedPageLinkRepository,
            sourcePageId: Long,
            sourceRevisionId: Long,
            sourceVisibility: Visibility,
            sourceSpaceId: Long,
            targetPageId: Long,
            linkId: Long,
            sourceAuthorId: Long = 100L,
            sourceUpdatedAt: Instant = DUMMY_INSTANT,
            deletedAt: Instant? = null
        ): Page {
            val page =
                basicPage(
                    id = PageId(sourcePageId),
                    spaceId = SpaceId(sourceSpaceId),
                    authorId = UserId(sourceAuthorId),
                    visibility = sourceVisibility,
                    currentVersion = 1,
                    updatedAt = sourceUpdatedAt,
                    deletedAt = deletedAt
                )
            pageRepository.save(page)
            revisionRepository.save(
                basicPageRevision(
                    id = PageRevisionId(sourceRevisionId),
                    pageId = PageId(sourcePageId),
                    version = 1,
                    authorId = UserId(sourceAuthorId)
                )
            )
            linkRepository.saveAll(
                listOf(
                    basicPageLink(
                        id = PageLinkId(linkId),
                        pageId = PageId(sourcePageId),
                        revisionId = PageRevisionId(sourceRevisionId),
                        target = PageId(targetPageId)
                    )
                )
            )
            return page
        }
    }
}

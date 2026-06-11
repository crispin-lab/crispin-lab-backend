package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageLink
import com.crispinlab.space.domain.page.PageLink.Target
import com.crispinlab.space.domain.page.PageLinkId
import com.crispinlab.space.domain.page.PageRevisionId
import com.crispinlab.space.testsupport.Fixtures.basicPageLink
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.net.URI
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedPageLinkRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedPageLinkRepository()

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("ExposedPageLinkRepository") {
            it("saveAll(emptyList) 은 아무 것도 저장하지 않는다") {
                transaction(database) {
                    repository.saveAll(emptyList())
                }

                transaction(database) {
                    repository.findByPageId(PageId(10L)).shouldBeEmpty()
                }
            }

            it("Internal target 을 saveAll 후 findByRevisionId 로 복원한다") {
                val links: List<PageLink> =
                    listOf(
                        basicPageLink(
                            id = PageLinkId(1L),
                            pageId = PageId(10L),
                            revisionId = PageRevisionId(100L),
                            target = Target.Internal(PageId(20L))
                        ),
                        basicPageLink(
                            id = PageLinkId(2L),
                            pageId = PageId(10L),
                            revisionId = PageRevisionId(100L),
                            target = Target.Internal(PageId(21L))
                        )
                    )
                transaction(database) {
                    repository.saveAll(links)
                }

                transaction(database) {
                    val found = repository.findByRevisionId(PageRevisionId(100L))
                    found shouldHaveSize 2
                    found.map { it.target } shouldContainExactlyInAnyOrder
                        listOf(
                            Target.Internal(PageId(20L)),
                            Target.Internal(PageId(21L))
                        )
                }
            }

            it("External target 을 saveAll 후 url 그대로 복원한다") {
                val links: List<PageLink> =
                    listOf(
                        basicPageLink(
                            id = PageLinkId(3L),
                            pageId = PageId(11L),
                            revisionId = PageRevisionId(101L),
                            target = Target.External(URI.create("https://example.com/x"))
                        )
                    )
                transaction(database) {
                    repository.saveAll(links)
                }

                transaction(database) {
                    val found = repository.findByRevisionId(PageRevisionId(101L)).single()
                    found.target shouldBe
                        Target.External(URI.create("https://example.com/x"))
                }
            }

            it("findByPageId 는 page 의 모든 revision 링크를 반환한다") {
                val links: List<PageLink> =
                    listOf(
                        basicPageLink(
                            id = PageLinkId(11L),
                            pageId = PageId(20L),
                            revisionId = PageRevisionId(101L)
                        ),
                        basicPageLink(
                            id = PageLinkId(12L),
                            pageId = PageId(20L),
                            revisionId = PageRevisionId(102L)
                        ),
                        basicPageLink(
                            id = PageLinkId(13L),
                            pageId = PageId(21L),
                            revisionId = PageRevisionId(101L)
                        )
                    )
                transaction(database) {
                    repository.saveAll(links)
                }

                transaction(database) {
                    repository.findByPageId(PageId(20L)) shouldHaveSize 2
                    repository.findByPageId(PageId(21L)) shouldHaveSize 1
                }
            }
        }
    })

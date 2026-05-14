package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageLink
import com.crispinlab.space.domain.page.PageLinkId
import com.crispinlab.space.domain.page.PageRevisionId
import com.crispinlab.space.testsupport.Fixtures.basicPageLink
import com.crispinlab.space.testsupport.PostgresTestContext
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
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

            it("saveAll 후 findByRevisionId 로 같은 revision 의 링크만 반환된다") {
                val links: List<PageLink> =
                    listOf(
                        basicPageLink(
                            id = PageLinkId(1L),
                            pageId = PageId(10L),
                            revisionId = PageRevisionId(100L),
                            target = "foo"
                        ),
                        basicPageLink(
                            id = PageLinkId(2L),
                            pageId = PageId(10L),
                            revisionId = PageRevisionId(100L),
                            target = "bar"
                        ),
                        basicPageLink(
                            id = PageLinkId(3L),
                            pageId = PageId(10L),
                            revisionId = PageRevisionId(200L),
                            target = "baz"
                        )
                    )
                transaction(database) {
                    repository.saveAll(links)
                }

                transaction(database) {
                    val rev100 = repository.findByRevisionId(PageRevisionId(100L))
                    rev100 shouldHaveSize 2
                    rev100.map { it.target } shouldContainExactlyInAnyOrder listOf("foo", "bar")
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

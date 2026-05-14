package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageRevisionId
import com.crispinlab.space.testsupport.Fixtures.basicPageRevision
import com.crispinlab.space.testsupport.PostgresTestContext
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedPageRevisionRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedPageRevisionRepository()

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("ExposedPageRevisionRepository") {
            it("save 후 findBy 로 복원된다") {
                transaction(database) {
                    repository.save(
                        basicPageRevision(
                            id = PageRevisionId(1L),
                            pageId = PageId(10L),
                            version = 1
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findBy(PageRevisionId(1L)).shouldNotBeNull()
                    found.pageId shouldBe PageId(10L)
                    found.version shouldBe 1
                }
            }

            it("findLatestByPageId 는 가장 큰 version 을 반환한다") {
                transaction(database) {
                    repository.save(
                        basicPageRevision(
                            id = PageRevisionId(10L),
                            pageId = PageId(20L),
                            version = 1
                        )
                    )
                    repository.save(
                        basicPageRevision(
                            id = PageRevisionId(11L),
                            pageId = PageId(20L),
                            version = 2
                        )
                    )
                    repository.save(
                        basicPageRevision(
                            id = PageRevisionId(12L),
                            pageId = PageId(20L),
                            version = 3
                        )
                    )
                }

                transaction(database) {
                    val latest = repository.findLatestByPageId(PageId(20L)).shouldNotBeNull()
                    latest.version shouldBe 3
                }
            }

            it("findByPageId 는 version 내림차순으로 반환한다") {
                transaction(database) {
                    repository.save(
                        basicPageRevision(
                            id = PageRevisionId(20L),
                            pageId = PageId(30L),
                            version = 1
                        )
                    )
                    repository.save(
                        basicPageRevision(
                            id = PageRevisionId(21L),
                            pageId = PageId(30L),
                            version = 2
                        )
                    )
                }

                transaction(database) {
                    val revisions = repository.findByPageId(PageId(30L))
                    revisions shouldHaveSize 2
                    revisions.map { it.version } shouldBe listOf(2, 1)
                }
            }
        }
    })

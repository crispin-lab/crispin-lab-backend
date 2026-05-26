package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.PageRevisionId
import com.crispinlab.space.testsupport.Fixtures.basicPageRevision
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
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

            it("findBy(pageId, version) 으로 단건 복원된다") {
                transaction(database) {
                    repository.save(
                        basicPageRevision(
                            id = PageRevisionId(40L),
                            pageId = PageId(50L),
                            version = 1,
                            title = "초안"
                        )
                    )
                    repository.save(
                        basicPageRevision(
                            id = PageRevisionId(41L),
                            pageId = PageId(50L),
                            version = 2,
                            title = "두 번째"
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findBy(PageId(50L), version = 2).shouldNotBeNull()
                    found.id shouldBe PageRevisionId(41L)
                    found.title shouldBe "두 번째"
                }
            }

            it("findBy(pageId, version) 은 매칭이 없으면 null 을 반환한다") {
                transaction(database) {
                    repository.findBy(PageId(999L), version = 1).shouldBeNull()
                }
            }

            it("findByPageId 는 version 내림차순으로 페이징한다") {
                transaction(database) {
                    (1..5).forEach { version ->
                        repository.save(
                            basicPageRevision(
                                id = PageRevisionId(100L + version),
                                pageId = PageId(30L),
                                version = version
                            )
                        )
                    }
                }

                transaction(database) {
                    val firstPage =
                        repository.findByPageId(
                            PageId(30L),
                            PageRequest(page = 0, size = 2)
                        )
                    firstPage.items shouldHaveSize 2
                    firstPage.items.map { it.version } shouldBe listOf(5, 4)
                    firstPage.totalElements shouldBe 5L
                    firstPage.totalPages shouldBe 3
                    firstPage.hasNext shouldBe true

                    val lastPage =
                        repository.findByPageId(
                            PageId(30L),
                            PageRequest(page = 2, size = 2)
                        )
                    lastPage.items.map { it.version } shouldBe listOf(1)
                    lastPage.hasNext shouldBe false
                }
            }
        }
    })

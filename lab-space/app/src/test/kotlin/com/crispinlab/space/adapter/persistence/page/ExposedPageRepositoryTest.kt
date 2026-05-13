package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Fixtures.basicPage
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedPageRepositoryTest :
    DescribeSpec({
        val database =
            Database.connect(
                url = "jdbc:h2:mem:pages-test;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver"
            )
        val repository = ExposedPageRepository()

        beforeSpec {
            transaction(database) {
                SchemaUtils.create(Pages)
            }
        }

        afterEach {
            transaction(database) {
                Pages.deleteAll()
            }
        }

        describe("ExposedPageRepository") {
            it("save 후 별도 트랜잭션의 findBy 로 동일 entity 가 복원된다") {
                transaction(database) {
                    repository.save(
                        basicPage(
                            id = PageId(1L),
                            spaceId = SpaceId(10L),
                            title = "오늘의 회고",
                            content = PageContent("본문"),
                            visibility = Visibility.PUBLIC
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findBy(PageId(1L))

                    found.shouldNotBeNull()
                    found.id shouldBe PageId(1L)
                    found.spaceId shouldBe SpaceId(10L)
                    found.title shouldBe "오늘의 회고"
                    found.content.raw shouldBe "본문"
                    found.visibility shouldBe Visibility.PUBLIC
                }
            }

            it("같은 ID 로 다시 save 하면 update 가 일어난다") {
                transaction(database) {
                    repository.save(basicPage(id = PageId(2L), title = "이전"))
                }

                transaction(database) {
                    repository.save(
                        basicPage(
                            id = PageId(2L),
                            title = "새 제목",
                            content = PageContent("새 본문"),
                            currentVersion = 2
                        )
                    )
                }

                transaction(database) {
                    val updated = repository.findBy(PageId(2L)).shouldNotBeNull()
                    updated.title shouldBe "새 제목"
                    updated.content.raw shouldBe "새 본문"
                    updated.currentVersion shouldBe 2
                }
            }

            it("findChildren 은 parentPageId 가 일치하는 페이지를 반환한다") {
                transaction(database) {
                    repository.save(basicPage(id = PageId(3L), parentPageId = PageId(1L)))
                    repository.save(basicPage(id = PageId(4L), parentPageId = PageId(1L)))
                    repository.save(basicPage(id = PageId(5L), parentPageId = null))
                }

                transaction(database) {
                    repository.findChildren(PageId(1L)) shouldHaveSize 2
                }
            }

            it("findRoots 는 같은 스페이스의 parentPageId 가 null 인 페이지만 반환한다") {
                transaction(database) {
                    repository.save(
                        basicPage(id = PageId(6L), spaceId = SpaceId(20L), parentPageId = null)
                    )
                    repository.save(
                        basicPage(
                            id = PageId(7L),
                            spaceId = SpaceId(20L),
                            parentPageId = PageId(6L)
                        )
                    )
                    repository.save(
                        basicPage(id = PageId(8L), spaceId = SpaceId(21L), parentPageId = null)
                    )
                }

                transaction(database) {
                    val roots = repository.findRoots(SpaceId(20L))
                    roots shouldHaveSize 1
                    roots.first().id shouldBe PageId(6L)
                }
            }

            it("delete 후에는 findBy 가 null 을 반환한다") {
                transaction(database) {
                    repository.save(basicPage(id = PageId(9L)))
                }

                transaction(database) {
                    repository.delete(PageId(9L))
                }

                transaction(database) {
                    repository.findBy(PageId(9L)).shouldBeNull()
                }
            }
        }
    })

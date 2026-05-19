package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.PostgresTestContext
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedPageRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedPageRepository()

        afterEach {
            PostgresTestContext.truncateAll()
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

            it("repository.delete 는 soft delete 로 동작 — row 는 보존되고 findBy 는 null 을 반환한다") {
                transaction(database) {
                    repository.save(basicPage(id = PageId(9L)))
                }

                transaction(database) {
                    repository.delete(PageId(9L))
                }

                transaction(database) {
                    repository.findBy(PageId(9L)).shouldBeNull()
                    val row =
                        Pages
                            .selectAll()
                            .where { Pages.id eq 9L }
                            .firstOrNull()
                            .shouldNotBeNull()
                    row[Pages.deletedAt].shouldNotBeNull()
                }
            }

            it("findChildren 은 soft deleted 자식 페이지를 자동 제외한다") {
                transaction(database) {
                    repository.save(basicPage(id = PageId(70L), parentPageId = PageId(60L)))
                    repository.save(basicPage(id = PageId(71L), parentPageId = PageId(60L)))
                }

                transaction(database) {
                    repository.delete(PageId(70L))
                }

                transaction(database) {
                    val children = repository.findChildren(PageId(60L))
                    children shouldHaveSize 1
                    children.first().id shouldBe PageId(71L)
                }
            }

            it("findRoots 는 soft deleted 루트 페이지를 자동 제외한다") {
                transaction(database) {
                    repository.save(
                        basicPage(id = PageId(80L), spaceId = SpaceId(80L), parentPageId = null)
                    )
                    repository.save(
                        basicPage(id = PageId(81L), spaceId = SpaceId(80L), parentPageId = null)
                    )
                }

                transaction(database) {
                    repository.delete(PageId(80L))
                }

                transaction(database) {
                    val roots = repository.findRoots(SpaceId(80L))
                    roots shouldHaveSize 1
                    roots.first().id shouldBe PageId(81L)
                }
            }

            it("이미 soft delete 된 row 에 delete 를 다시 호출해도 deletedAt 이 갱신되지 않는다") {
                val firstDeletedAt =
                    transaction(database) {
                        repository.save(basicPage(id = PageId(90L)))
                        repository.delete(PageId(90L))
                        Pages
                            .selectAll()
                            .where { Pages.id eq 90L }
                            .first()[Pages.deletedAt]
                    }.shouldNotBeNull()

                transaction(database) {
                    repository.delete(PageId(90L))
                }

                transaction(database) {
                    val row =
                        Pages
                            .selectAll()
                            .where { Pages.id eq 90L }
                            .first()
                    row[Pages.deletedAt] shouldBe firstDeletedAt
                }
            }

            it("save 가 soft delete 된 row 의 deleted_at 을 덮지 않는다") {
                val originalDeletedAt =
                    transaction(database) {
                        repository.save(basicPage(id = PageId(100L)))
                        repository.delete(PageId(100L))
                        Pages
                            .selectAll()
                            .where { Pages.id eq 100L }
                            .first()[Pages.deletedAt]
                    }.shouldNotBeNull()

                transaction(database) {
                    repository.save(
                        basicPage(id = PageId(100L), title = "복구 시도", deletedAt = null)
                    )
                }

                transaction(database) {
                    val row =
                        Pages
                            .selectAll()
                            .where { Pages.id eq 100L }
                            .first()
                    row[Pages.deletedAt] shouldBe originalDeletedAt
                    repository.findBy(PageId(100L)).shouldBeNull()
                }
            }

            it("save 는 immutable 컬럼 (spaceId / authorId / createdAt) 을 덮지 않는다") {
                transaction(database) {
                    repository.save(
                        basicPage(
                            id = PageId(110L),
                            spaceId = SpaceId(800L),
                            authorId = UserId(900L),
                            createdAt = DUMMY_INSTANT
                        )
                    )
                }

                transaction(database) {
                    repository.save(
                        basicPage(
                            id = PageId(110L),
                            spaceId = SpaceId(801L),
                            authorId = UserId(901L),
                            createdAt = DUMMY_INSTANT.plusSeconds(60),
                            title = "수정 시도"
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findBy(PageId(110L)).shouldNotBeNull()
                    found.spaceId shouldBe SpaceId(800L)
                    found.authorId shouldBe UserId(900L)
                    found.createdAt shouldBe DUMMY_INSTANT
                    found.title shouldBe "수정 시도"
                }
            }
        }
    })

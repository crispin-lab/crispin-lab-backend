package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.domain.page.PageContent
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.seedPublicSpaces
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
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

            it("nextDisplayOrderIn 은 빈 scope 에서 0 을 반환한다") {
                transaction(database) {
                    repository.nextDisplayOrderIn(SpaceId(200L), null) shouldBe 0
                    repository.nextDisplayOrderIn(SpaceId(200L), PageId(999L)) shouldBe 0
                }
            }

            it("nextDisplayOrderIn 은 같은 (spaceId, parentPageId) scope 의 MAX + 1 을 반환한다") {
                transaction(database) {
                    repository.save(
                        basicPage(
                            id = PageId(201L),
                            spaceId = SpaceId(201L),
                            parentPageId = null,
                            displayOrder = 0
                        )
                    )
                    repository.save(
                        basicPage(
                            id = PageId(202L),
                            spaceId = SpaceId(201L),
                            parentPageId = null,
                            displayOrder = 3
                        )
                    )
                    repository.save(
                        basicPage(
                            id = PageId(203L),
                            spaceId = SpaceId(201L),
                            parentPageId = PageId(201L),
                            displayOrder = 7
                        )
                    )
                }

                transaction(database) {
                    repository.nextDisplayOrderIn(SpaceId(201L), null) shouldBe 4
                    repository.nextDisplayOrderIn(SpaceId(201L), PageId(201L)) shouldBe 8
                }
            }

            it("nextDisplayOrderIn 은 다른 스페이스의 displayOrder 를 포함하지 않는다") {
                transaction(database) {
                    repository.save(
                        basicPage(
                            id = PageId(210L),
                            spaceId = SpaceId(210L),
                            parentPageId = null,
                            displayOrder = 9
                        )
                    )
                }

                transaction(database) {
                    repository.nextDisplayOrderIn(SpaceId(211L), null) shouldBe 0
                }
            }

            it("nextDisplayOrderIn 은 soft deleted 페이지를 제외한 MAX 를 반환한다") {
                transaction(database) {
                    repository.save(
                        basicPage(
                            id = PageId(220L),
                            spaceId = SpaceId(220L),
                            parentPageId = null,
                            displayOrder = 0
                        )
                    )
                    repository.save(
                        basicPage(
                            id = PageId(221L),
                            spaceId = SpaceId(220L),
                            parentPageId = null,
                            displayOrder = 5
                        )
                    )
                }

                transaction(database) {
                    repository.delete(PageId(221L))
                }

                transaction(database) {
                    repository.nextDisplayOrderIn(SpaceId(220L), null) shouldBe 1
                }
            }

            it("save / findBy 가 displayOrder 를 그대로 보존한다") {
                transaction(database) {
                    repository.save(
                        basicPage(
                            id = PageId(230L),
                            spaceId = SpaceId(230L),
                            displayOrder = 12
                        )
                    )
                }

                transaction(database) {
                    repository.findBy(PageId(230L)).shouldNotBeNull().displayOrder shouldBe 12
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

            it("findVisibilitiesByIds 는 다중 id 를 한 번에 매핑한다") {
                seedPublicSpaces(database, 300L, 301L)
                transaction(database) {
                    repository.save(
                        basicPage(
                            id = PageId(300L),
                            spaceId = SpaceId(300L),
                            authorId = UserId(900L),
                            visibility = Visibility.PUBLIC
                        )
                    )
                    repository.save(
                        basicPage(
                            id = PageId(301L),
                            spaceId = SpaceId(301L),
                            authorId = UserId(901L),
                            visibility = Visibility.INTERNAL
                        )
                    )
                }

                transaction(database) {
                    val records =
                        repository.findVisibilitiesByIds(listOf(PageId(300L), PageId(301L)))

                    records.size shouldBe 2
                    records[PageId(300L)]?.visibility shouldBe Visibility.PUBLIC
                    records[PageId(300L)]?.spaceId shouldBe SpaceId(300L)
                    records[PageId(300L)]?.authorId shouldBe UserId(900L)
                    records[PageId(301L)]?.visibility shouldBe Visibility.INTERNAL
                }
            }

            it("findVisibilitiesByIds 는 빈 입력에 빈 map 을 반환한다") {
                transaction(database) {
                    repository.findVisibilitiesByIds(emptyList()).shouldBeEmpty()
                }
            }

            it("findVisibilitiesByIds 는 soft deleted 페이지를 제외한다") {
                seedPublicSpaces(database, 10L)
                transaction(database) {
                    repository.save(
                        basicPage(id = PageId(310L), visibility = Visibility.PUBLIC)
                    )
                    repository.delete(PageId(310L))
                }

                transaction(database) {
                    repository.findVisibilitiesByIds(listOf(PageId(310L))).shouldBeEmpty()
                }
            }

            it("findVisibilitiesByIds 는 alive 와 soft deleted 가 섞인 입력에서 alive 만 반환한다") {
                seedPublicSpaces(database, 10L)
                transaction(database) {
                    repository.save(basicPage(id = PageId(320L), visibility = Visibility.PUBLIC))
                    repository.save(basicPage(id = PageId(321L), visibility = Visibility.INTERNAL))
                    repository.delete(PageId(321L))
                }

                transaction(database) {
                    val records =
                        repository.findVisibilitiesByIds(
                            listOf(PageId(320L), PageId(321L))
                        )
                    records.keys shouldBe setOf(PageId(320L))
                    records[PageId(320L)]?.visibility shouldBe Visibility.PUBLIC
                }
            }

            it("findVisibilitiesByIds 는 soft-deleted space 의 page 를 결과에서 제외한다 (cascade)") {
                seedPublicSpaces(database, 60L)
                transaction(database) {
                    repository.save(
                        basicPage(
                            id = PageId(330L),
                            spaceId = SpaceId(60L),
                            visibility = Visibility.PUBLIC
                        )
                    )
                }

                transaction(database) {
                    TransactionManager
                        .current()
                        .exec("UPDATE spaces SET deleted_at = NOW() WHERE id = 60")
                }

                transaction(database) {
                    repository.findVisibilitiesByIds(listOf(PageId(330L))).shouldBeEmpty()
                }
            }
        }
    })

package com.crispinlab.space.adapter.persistence.page

import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.seedPublicSpaces
import com.crispinlab.space.testsupport.seedSpaces
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedPageAncestorAdapterTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedPageRepository()
        val adapter = ExposedPageAncestorAdapter()

        beforeEach {
            seedPublicSpaces(database, 10L, 11L, 99L)
        }

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("ExposedPageAncestorAdapter") {
            it("target 이 root 면 빈 list 를 반환한다") {
                transaction(database) {
                    repository.save(basicPage(id = PageId(1L), parentPageId = null))
                }

                transaction(database) {
                    adapter.findAncestorsOf(PageId(1L)).shouldBeEmpty()
                }
            }

            it("3단계 체인이면 root → 직계 부모 순서로 반환한다") {
                transaction(database) {
                    repository.save(basicPage(id = PageId(1L), title = "root"))
                    repository.save(
                        basicPage(id = PageId(2L), parentPageId = PageId(1L), title = "mid")
                    )
                    repository.save(
                        basicPage(id = PageId(3L), parentPageId = PageId(2L), title = "leaf")
                    )
                }

                transaction(database) {
                    val ancestors = adapter.findAncestorsOf(PageId(3L))

                    ancestors shouldHaveSize 2
                    ancestors[0].pageId shouldBe PageId(1L)
                    ancestors[0].title shouldBe "root"
                    ancestors[1].pageId shouldBe PageId(2L)
                    ancestors[1].title shouldBe "mid"
                }
            }

            it(
                "ancestor 의 metadata (spaceId / spaceVisibility / authorId / visibility) 도 함께 반환한다"
            ) {
                val root =
                    basicPage(
                        id = PageId(1L),
                        spaceId = SpaceId(11L),
                        authorId = UserId(101L),
                        visibility = Visibility.PUBLIC
                    )
                val leaf =
                    basicPage(
                        id = PageId(2L),
                        spaceId = SpaceId(11L),
                        authorId = UserId(102L),
                        parentPageId = PageId(1L),
                        visibility = Visibility.INTERNAL
                    )
                transaction(database) {
                    repository.save(root)
                    repository.save(leaf)
                }

                transaction(database) {
                    val ancestors = adapter.findAncestorsOf(PageId(2L))

                    ancestors shouldHaveSize 1
                    ancestors[0].pageId shouldBe root.id
                    ancestors[0].spaceId shouldBe root.spaceId
                    ancestors[0].spaceVisibility shouldBe SpaceVisibility.PUBLIC
                    ancestors[0].authorId shouldBe root.authorId
                    ancestors[0].visibility shouldBe root.visibility
                }
            }

            it("중간 ancestor 가 soft delete 되어 있으면 recursion 이 끊겨 빈 list 가 된다") {
                transaction(database) {
                    repository.save(basicPage(id = PageId(1L), title = "root"))
                    repository.save(
                        basicPage(id = PageId(2L), parentPageId = PageId(1L), title = "mid")
                    )
                    repository.save(
                        basicPage(id = PageId(3L), parentPageId = PageId(2L), title = "leaf")
                    )
                }

                transaction(database) {
                    repository.delete(PageId(2L))
                }

                transaction(database) {
                    adapter.findAncestorsOf(PageId(3L)).shouldBeEmpty()
                }
            }

            it("root 만 soft delete 되어 있으면 chain 이 mid 에서 끊긴다") {
                transaction(database) {
                    repository.save(basicPage(id = PageId(1L), title = "root"))
                    repository.save(
                        basicPage(id = PageId(2L), parentPageId = PageId(1L), title = "mid")
                    )
                    repository.save(
                        basicPage(id = PageId(3L), parentPageId = PageId(2L), title = "leaf")
                    )
                }

                transaction(database) {
                    repository.delete(PageId(1L))
                }

                transaction(database) {
                    val ancestors = adapter.findAncestorsOf(PageId(3L))

                    ancestors shouldHaveSize 1
                    ancestors[0].pageId shouldBe PageId(2L)
                    ancestors[0].title shouldBe "mid"
                }
            }

            it("다른 스페이스의 페이지를 부모로 가리키는 row 가 있어도 chain 에 끌어오지 않는다") {
                transaction(database) {
                    repository.save(
                        basicPage(
                            id = PageId(1L),
                            spaceId = SpaceId(99L),
                            title = "타 스페이스 root"
                        )
                    )
                    repository.save(
                        basicPage(
                            id = PageId(2L),
                            spaceId = SpaceId(10L),
                            parentPageId = PageId(1L),
                            title = "leaf"
                        )
                    )
                }

                transaction(database) {
                    adapter.findAncestorsOf(PageId(2L)).shouldBeEmpty()
                }
            }

            it("target 자체가 soft delete 되어 있으면 빈 list 를 반환한다") {
                transaction(database) {
                    repository.save(basicPage(id = PageId(1L)))
                    repository.save(basicPage(id = PageId(2L), parentPageId = PageId(1L)))
                }

                transaction(database) {
                    repository.delete(PageId(2L))
                }

                transaction(database) {
                    adapter.findAncestorsOf(PageId(2L)).shouldBeEmpty()
                }
            }

            it("target 의 space 가 soft delete 되어 있으면 anchor 가 빈 결과를 만든다") {
                seedSpaces(database, 50L to SpaceVisibility.PUBLIC)
                transaction(database) {
                    repository.save(
                        basicPage(id = PageId(1L), spaceId = SpaceId(50L), title = "root")
                    )
                    repository.save(
                        basicPage(
                            id = PageId(2L),
                            spaceId = SpaceId(50L),
                            parentPageId = PageId(1L),
                            title = "leaf"
                        )
                    )
                }

                transaction(database) {
                    TransactionManager
                        .current()
                        .exec(
                            "UPDATE spaces SET deleted_at = NOW() WHERE id = 50"
                        )
                }

                transaction(database) {
                    adapter.findAncestorsOf(PageId(2L)).shouldBeEmpty()
                }
            }

            it("chain 중간 페이지의 space 가 soft delete 되어 있으면 그 지점에서 recursion 이 끊긴다") {
                seedSpaces(
                    database,
                    51L to SpaceVisibility.PUBLIC,
                    52L to SpaceVisibility.PUBLIC
                )
                transaction(database) {
                    repository.save(
                        basicPage(id = PageId(1L), spaceId = SpaceId(51L), title = "root")
                    )
                    repository.save(
                        basicPage(
                            id = PageId(2L),
                            spaceId = SpaceId(51L),
                            parentPageId = PageId(1L),
                            title = "leaf"
                        )
                    )
                }

                transaction(database) {
                    TransactionManager
                        .current()
                        .exec(
                            "UPDATE spaces SET deleted_at = NOW() WHERE id = 51"
                        )
                }

                transaction(database) {
                    adapter.findAncestorsOf(PageId(2L)).shouldBeEmpty()
                }
            }

            it("깊이 가드로 64 단계까지만 반환한다") {
                transaction(database) {
                    (1L..70L).forEach { idx ->
                        repository.save(
                            basicPage(
                                id = PageId(idx),
                                parentPageId = if (idx == 1L) null else PageId(idx - 1)
                            )
                        )
                    }
                }

                transaction(database) {
                    val ancestors = adapter.findAncestorsOf(PageId(70L))

                    ancestors shouldHaveSize 64
                    ancestors.first().pageId shouldBe PageId(6L)
                    ancestors.last().pageId shouldBe PageId(69L)
                }
            }
        }
    })

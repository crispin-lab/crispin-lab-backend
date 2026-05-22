package com.crispinlab.space.adapter.persistence.space

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedSpaceRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedSpaceRepository()

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("ExposedSpaceRepository") {
            it("save 한 뒤 별도 transaction 의 findBy 로 동일 entity 가 복원된다") {
                transaction(database) {
                    repository.save(
                        basicSpace(id = SpaceId(1L), name = "팀 위키", description = "공유 공간")
                    )
                }

                transaction(database) {
                    val found = repository.findBy(SpaceId(1L))

                    found.shouldNotBeNull()
                    found.id shouldBe SpaceId(1L)
                    found.name shouldBe "팀 위키"
                    found.description shouldBe "공유 공간"
                }
            }

            it("같은 ID 로 다시 save 하면 update 가 일어난다") {
                transaction(database) {
                    repository.save(basicSpace(id = SpaceId(2L), name = "이전"))
                }

                transaction(database) {
                    val existing = repository.findBy(SpaceId(2L))!!
                    existing.edit(name = "새로운")
                    repository.save(existing)
                }

                transaction(database) {
                    repository.findBy(SpaceId(2L))?.name shouldBe "새로운"
                }
            }

            it("repository.delete 는 soft delete 로 동작 — row 는 보존되고 findBy 는 null 을 반환한다") {
                transaction(database) {
                    repository.save(basicSpace(id = SpaceId(5L)))
                }

                transaction(database) {
                    repository.delete(SpaceId(5L))
                }

                transaction(database) {
                    repository.findBy(SpaceId(5L)).shouldBeNull()
                    val row =
                        Spaces
                            .selectAll()
                            .where { Spaces.id eq 5L }
                            .firstOrNull()
                            .shouldNotBeNull()
                    row[Spaces.deletedAt].shouldNotBeNull()
                }
            }

            it("findPage 는 soft deleted 스페이스를 자동 제외한다") {
                transaction(database) {
                    repository.save(basicSpace(id = SpaceId(40L), createdAt = DUMMY_INSTANT))
                    repository.save(
                        basicSpace(id = SpaceId(41L), createdAt = DUMMY_INSTANT.plusSeconds(60))
                    )
                }

                transaction(database) {
                    repository.delete(SpaceId(40L))
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(page = 0, size = 10),
                            SpaceVisibility.entries.toSet()
                        )
                    result.totalElements shouldBe 1L
                    result.items.map { it.id } shouldBe listOf(SpaceId(41L))
                }
            }

            it("findPage 는 createdAt DESC 로 정렬해 페이지를 돌려준다") {
                transaction(database) {
                    repository.save(
                        basicSpace(
                            id = SpaceId(11L),
                            name = "오래된",
                            createdAt = DUMMY_INSTANT
                        )
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(12L),
                            name = "중간",
                            createdAt = DUMMY_INSTANT.plusSeconds(60)
                        )
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(13L),
                            name = "최근",
                            createdAt = DUMMY_INSTANT.plusSeconds(120)
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(
                                page = 0,
                                size = 10
                            ),
                            SpaceVisibility.entries.toSet()
                        )

                    result.totalElements shouldBe 3L
                    result.items.map { it.id } shouldBe
                        listOf(SpaceId(13L), SpaceId(12L), SpaceId(11L))
                }
            }

            it("findPage 는 offset/limit 을 반영한다") {
                transaction(database) {
                    (1..5).forEach { index ->
                        repository.save(
                            basicSpace(
                                id = SpaceId(20L + index),
                                name = "스페이스 $index",
                                createdAt = DUMMY_INSTANT.plusSeconds(index.toLong())
                            )
                        )
                    }
                }

                transaction(database) {
                    val secondPage =
                        repository.findPage(
                            PageRequest(
                                page = 1,
                                size = 2
                            ),
                            SpaceVisibility.entries.toSet()
                        )

                    secondPage.page shouldBe 1
                    secondPage.size shouldBe 2
                    secondPage.totalElements shouldBe 5L
                    secondPage.items.map { it.id } shouldBe
                        listOf(SpaceId(23L), SpaceId(22L))
                }
            }

            it("findPage 는 데이터가 없으면 빈 페이지를 돌려준다") {
                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(
                                page = 0,
                                size = 10
                            ),
                            SpaceVisibility.entries.toSet()
                        )

                    result.items shouldBe emptyList()
                    result.totalElements shouldBe 0L
                }
            }

            it("save 가 soft delete 된 row 의 deleted_at 을 덮지 않는다") {
                val originalDeletedAt =
                    transaction(database) {
                        repository.save(basicSpace(id = SpaceId(100L)))
                        repository.delete(SpaceId(100L))
                        Spaces
                            .selectAll()
                            .where { Spaces.id eq 100L }
                            .first()[Spaces.deletedAt]
                    }.shouldNotBeNull()

                transaction(database) {
                    repository.save(
                        basicSpace(id = SpaceId(100L), name = "복구 시도", deletedAt = null)
                    )
                }

                transaction(database) {
                    val row =
                        Spaces
                            .selectAll()
                            .where { Spaces.id eq 100L }
                            .first()
                    row[Spaces.deletedAt] shouldBe originalDeletedAt
                    repository.findBy(SpaceId(100L)).shouldBeNull()
                }
            }

            it("save 는 immutable 컬럼 (createdAt) 을 덮지 않는다") {
                transaction(database) {
                    repository.save(
                        basicSpace(id = SpaceId(110L), name = "원본", createdAt = DUMMY_INSTANT)
                    )
                }

                transaction(database) {
                    repository.save(
                        basicSpace(
                            id = SpaceId(110L),
                            name = "수정 시도",
                            createdAt = DUMMY_INSTANT.plusSeconds(60)
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findBy(SpaceId(110L)).shouldNotBeNull()
                    found.name shouldBe "수정 시도"
                    found.createdAt shouldBe DUMMY_INSTANT
                }
            }

            it("findPage 는 createdAt 이 동일하면 id DESC 로 결정적으로 정렬한다") {
                transaction(database) {
                    repository.save(
                        basicSpace(
                            id = SpaceId(31L),
                            name = "첫번째",
                            createdAt = DUMMY_INSTANT
                        )
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(32L),
                            name = "두번째",
                            createdAt = DUMMY_INSTANT
                        )
                    )
                    repository.save(
                        basicSpace(
                            id = SpaceId(33L),
                            name = "세번째",
                            createdAt = DUMMY_INSTANT
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findPage(
                            PageRequest(
                                page = 0,
                                size = 10
                            ),
                            SpaceVisibility.entries.toSet()
                        )

                    result.items.map { it.id } shouldBe
                        listOf(SpaceId(33L), SpaceId(32L), SpaceId(31L))
                }
            }
        }
    })

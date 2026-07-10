package com.crispinlab.space.adapter.persistence.visit

import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.visit.SpaceVisitId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicSpaceVisit
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedSpaceVisitRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedSpaceVisitRepository()

        afterEach { PostgresTestContext.truncateAll() }

        describe("ExposedSpaceVisitRepository") {
            it("save 후 findByUserIdAndSpaceId 로 동일 entity 가 복원된다") {
                transaction(database) {
                    repository.save(
                        basicSpaceVisit(
                            id = SpaceVisitId(1L),
                            userId = UserId(100L),
                            spaceId = SpaceId(10L),
                            lastVisitedAt = DUMMY_INSTANT
                        )
                    )
                }

                transaction(database) {
                    val found =
                        repository
                            .findByUserIdAndSpaceId(UserId(100L), SpaceId(10L))
                            .shouldNotBeNull()
                    found.id shouldBe SpaceVisitId(1L)
                    found.userId shouldBe UserId(100L)
                    found.spaceId shouldBe SpaceId(10L)
                    found.lastVisitedAt shouldBe DUMMY_INSTANT
                }
            }

            it("동일 (userId, spaceId) 로 다시 save 하면 lastVisitedAt 만 갱신되고 id 는 기존 값 유지된다") {
                transaction(database) {
                    repository.save(
                        basicSpaceVisit(
                            id = SpaceVisitId(2L),
                            userId = UserId(200L),
                            spaceId = SpaceId(20L),
                            lastVisitedAt = DUMMY_INSTANT
                        )
                    )
                }

                val later = DUMMY_INSTANT.plusSeconds(3600)
                transaction(database) {
                    repository.save(
                        basicSpaceVisit(
                            id = SpaceVisitId(999L),
                            userId = UserId(200L),
                            spaceId = SpaceId(20L),
                            lastVisitedAt = later
                        )
                    )
                }

                transaction(database) {
                    val found =
                        repository
                            .findByUserIdAndSpaceId(UserId(200L), SpaceId(20L))
                            .shouldNotBeNull()
                    found.id shouldBe SpaceVisitId(2L)
                    found.lastVisitedAt shouldBe later
                }
            }

            it("findByUserIdAndSpaceId 는 매핑이 없으면 null 을 반환한다") {
                transaction(database) {
                    repository.findByUserIdAndSpaceId(UserId(300L), SpaceId(30L)).shouldBeNull()
                }
            }

            it("findByUserIdAndSpaceIds 는 batch 로 SpaceVisit map 을 돌려주고 미방문 space 는 제외한다") {
                val visitedAtA = DUMMY_INSTANT
                val visitedAtB = DUMMY_INSTANT.plusSeconds(60)
                transaction(database) {
                    repository.save(
                        basicSpaceVisit(
                            id = SpaceVisitId(3L),
                            userId = UserId(400L),
                            spaceId = SpaceId(40L),
                            lastVisitedAt = visitedAtA
                        )
                    )
                    repository.save(
                        basicSpaceVisit(
                            id = SpaceVisitId(4L),
                            userId = UserId(400L),
                            spaceId = SpaceId(41L),
                            lastVisitedAt = visitedAtB
                        )
                    )
                    repository.save(
                        basicSpaceVisit(
                            id = SpaceVisitId(5L),
                            userId = UserId(401L),
                            spaceId = SpaceId(40L),
                            lastVisitedAt = visitedAtA
                        )
                    )
                }

                transaction(database) {
                    val found =
                        repository.findByUserIdAndSpaceIds(
                            userId = UserId(400L),
                            spaceIds = setOf(SpaceId(40L), SpaceId(41L), SpaceId(42L))
                        )
                    found.keys shouldBe setOf(SpaceId(40L), SpaceId(41L))
                    found[SpaceId(40L)]?.lastVisitedAt shouldBe visitedAtA
                    found[SpaceId(40L)]?.userId shouldBe UserId(400L)
                    found[SpaceId(41L)]?.lastVisitedAt shouldBe visitedAtB
                }
            }

            it("findByUserIdAndSpaceIds 는 빈 spaceIds 에 대해 emptyMap 반환한다") {
                transaction(database) {
                    repository.findByUserIdAndSpaceIds(
                        userId = UserId(500L),
                        spaceIds = emptyList()
                    ) shouldBe emptyMap()
                }
            }
        }
    })

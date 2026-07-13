package com.crispinlab.space.adapter.persistence.visit

import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.visit.SpaceVisitId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicSpaceVisit
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedSpaceVisitRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedSpaceVisitRepository()

        afterEach { PostgresTestContext.truncateAll() }

        describe("ExposedSpaceVisitRepository") {
            it("save 후 batch 조회로 저장된 entity 가 복원된다") {
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
                        repository.findByUserIdAndSpaceIds(
                            userId = UserId(100L),
                            spaceIds = setOf(SpaceId(10L))
                        )
                    val visit = found[SpaceId(10L)] ?: error("expected visit for space 10")
                    visit.id shouldBe SpaceVisitId(1L)
                    visit.userId shouldBe UserId(100L)
                    visit.spaceId shouldBe SpaceId(10L)
                    visit.lastVisitedAt shouldBe DUMMY_INSTANT
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
                    val visit =
                        repository.findByUserIdAndSpaceIds(
                            userId = UserId(200L),
                            spaceIds = setOf(SpaceId(20L))
                        )[SpaceId(20L)] ?: error("expected visit for space 20")
                    visit.id shouldBe SpaceVisitId(2L)
                    visit.lastVisitedAt shouldBe later
                }
            }

            it("out-of-order save 로 오래된 timestamp 가 들어와도 최신 값이 유지된다") {
                val newer = DUMMY_INSTANT.plusSeconds(3600)
                val older = DUMMY_INSTANT
                transaction(database) {
                    repository.save(
                        basicSpaceVisit(
                            id = SpaceVisitId(6L),
                            userId = UserId(600L),
                            spaceId = SpaceId(60L),
                            lastVisitedAt = newer
                        )
                    )
                }

                transaction(database) {
                    repository.save(
                        basicSpaceVisit(
                            id = SpaceVisitId(7L),
                            userId = UserId(600L),
                            spaceId = SpaceId(60L),
                            lastVisitedAt = older
                        )
                    )
                }

                transaction(database) {
                    val visit =
                        repository.findByUserIdAndSpaceIds(
                            userId = UserId(600L),
                            spaceIds = setOf(SpaceId(60L))
                        )[SpaceId(60L)] ?: error("expected visit for space 60")
                    visit.id shouldBe SpaceVisitId(6L)
                    visit.lastVisitedAt shouldBe newer
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

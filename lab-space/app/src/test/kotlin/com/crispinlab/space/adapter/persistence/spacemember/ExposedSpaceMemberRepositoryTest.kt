package com.crispinlab.space.adapter.persistence.spacemember

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicSpaceMember
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private fun awaitLockWaiter(
    database: Database,
    deadlineMs: Long = 5000
): Boolean {
    val deadline = System.currentTimeMillis() + deadlineMs
    while (System.currentTimeMillis() < deadline) {
        val waiterCount =
            transaction(database) {
                exec(
                    """
                    SELECT count(*)
                    FROM pg_stat_activity
                    WHERE wait_event_type = 'Lock'
                      AND datname = current_database()
                      AND pid <> pg_backend_pid()
                    """.trimIndent()
                ) { rs ->
                    if (rs.next()) rs.getLong(1) else 0L
                } ?: 0L
            }
        if (waiterCount > 0) return true
        Thread.sleep(50)
    }
    return false
}

class ExposedSpaceMemberRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedSpaceMemberRepository()

        afterEach { PostgresTestContext.truncateAll() }

        describe("ExposedSpaceMemberRepository") {
            it("save 후 findBy 로 동일 entity 가 복원된다") {
                transaction(database) {
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(1L),
                            spaceId = SpaceId(10L),
                            userId = UserId(100L),
                            role = SpaceMemberRole.OWNER,
                            joinedAt = DUMMY_INSTANT
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findBy(SpaceMemberId(1L)).shouldNotBeNull()
                    found.spaceId shouldBe SpaceId(10L)
                    found.userId shouldBe UserId(100L)
                    found.role shouldBe SpaceMemberRole.OWNER
                    found.joinedAt shouldBe DUMMY_INSTANT
                }
            }

            it("같은 ID 로 다시 save 하면 role 만 갱신되고 spaceId·userId·joinedAt 은 보존된다") {
                transaction(database) {
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(2L),
                            spaceId = SpaceId(10L),
                            userId = UserId(100L),
                            role = SpaceMemberRole.MEMBER,
                            joinedAt = DUMMY_INSTANT
                        )
                    )
                }

                transaction(database) {
                    val existing = repository.findBy(SpaceMemberId(2L)).shouldNotBeNull()
                    existing.changeRole(SpaceMemberRole.OWNER)
                    repository.save(existing)
                }

                transaction(database) {
                    val found = repository.findBy(SpaceMemberId(2L)).shouldNotBeNull()
                    found.role shouldBe SpaceMemberRole.OWNER
                    found.joinedAt shouldBe DUMMY_INSTANT
                    found.spaceId shouldBe SpaceId(10L)
                    found.userId shouldBe UserId(100L)
                }
            }

            it("동일 (spaceId, userId) 로 다른 id save 시 ConflictException") {
                transaction(database) {
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(3L),
                            spaceId = SpaceId(10L),
                            userId = UserId(100L)
                        )
                    )
                }

                shouldThrow<ConflictException> {
                    transaction(database) {
                        repository.save(
                            basicSpaceMember(
                                id = SpaceMemberId(4L),
                                spaceId = SpaceId(10L),
                                userId = UserId(100L)
                            )
                        )
                    }
                }
            }

            it("findBySpaceIdAndUserId 는 해당 매핑을 반환한다") {
                transaction(database) {
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(5L),
                            spaceId = SpaceId(20L),
                            userId = UserId(200L)
                        )
                    )
                }

                transaction(database) {
                    val found =
                        repository
                            .findBySpaceIdAndUserId(SpaceId(20L), UserId(200L))
                            .shouldNotBeNull()
                    found.id shouldBe SpaceMemberId(5L)
                }

                transaction(database) {
                    repository.findBySpaceIdAndUserId(SpaceId(20L), UserId(999L)).shouldBeNull()
                }
            }

            it("findBySpaceId 는 해당 스페이스의 멤버만 paging 으로 반환한다") {
                transaction(database) {
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(10L),
                            spaceId = SpaceId(30L),
                            userId = UserId(101L)
                        )
                    )
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(11L),
                            spaceId = SpaceId(30L),
                            userId = UserId(102L)
                        )
                    )
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(12L),
                            spaceId = SpaceId(40L),
                            userId = UserId(103L)
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findBySpaceId(
                            spaceId = SpaceId(30L),
                            pageRequest = PageRequest(page = 0, size = 10)
                        )
                    result.items shouldHaveSize 2
                    result.totalElements shouldBe 2L
                    result.items.all { it.spaceId == SpaceId(30L) } shouldBe true
                }
            }

            it("findSpaceIdsByUserId 는 사용자가 속한 모든 spaceId 를 반환한다") {
                transaction(database) {
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(20L),
                            spaceId = SpaceId(50L),
                            userId = UserId(300L)
                        )
                    )
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(21L),
                            spaceId = SpaceId(51L),
                            userId = UserId(300L)
                        )
                    )
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(22L),
                            spaceId = SpaceId(52L),
                            userId = UserId(301L)
                        )
                    )
                }

                transaction(database) {
                    repository.findSpaceIdsByUserId(UserId(300L)) shouldBe
                        setOf(SpaceId(50L), SpaceId(51L))
                    repository.findSpaceIdsByUserId(UserId(301L)) shouldBe setOf(SpaceId(52L))
                    repository.findSpaceIdsByUserId(UserId(999L)) shouldBe emptySet()
                }
            }

            it("countOwnersBy 는 OWNER 만 카운트한다") {
                transaction(database) {
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(30L),
                            spaceId = SpaceId(60L),
                            userId = UserId(401L),
                            role = SpaceMemberRole.OWNER
                        )
                    )
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(31L),
                            spaceId = SpaceId(60L),
                            userId = UserId(402L),
                            role = SpaceMemberRole.OWNER
                        )
                    )
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(32L),
                            spaceId = SpaceId(60L),
                            userId = UserId(403L),
                            role = SpaceMemberRole.MEMBER
                        )
                    )
                }

                transaction(database) {
                    repository.countOwnersBy(SpaceId(60L)) shouldBe 2L
                }
            }

            it("delete 는 row 를 hard delete 한다") {
                transaction(database) {
                    repository.save(
                        basicSpaceMember(id = SpaceMemberId(40L), spaceId = SpaceId(70L))
                    )
                }

                transaction(database) {
                    repository.delete(SpaceMemberId(40L))
                }

                transaction(database) {
                    repository.findBy(SpaceMemberId(40L)).shouldBeNull()
                }
            }

            it("findSpaceIdsByUserId 가 빈 set 을 다룬다") {
                transaction(database) {
                    repository.findSpaceIdsByUserId(UserId(0L)) shouldBe emptySet()
                }
            }

            it("countOwnersBy 는 다른 트랜잭션을 commit 까지 wait 시킨다 — FOR UPDATE race 보호") {
                transaction(database) {
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(90L),
                            spaceId = SpaceId(90L),
                            userId = UserId(901L),
                            role = SpaceMemberRole.OWNER
                        )
                    )
                }

                val executor = Executors.newFixedThreadPool(2)
                val t1Locked = CountDownLatch(1)
                val t1Proceed = CountDownLatch(1)
                val t2Acquired = CountDownLatch(1)
                try {
                    executor.submit {
                        transaction(database) {
                            repository.countOwnersBy(SpaceId(90L))
                            t1Locked.countDown()
                            t1Proceed.await(5, SECONDS)
                        }
                    }
                    t1Locked.await(5, SECONDS) shouldBe true

                    executor.submit {
                        transaction(database) {
                            repository.countOwnersBy(SpaceId(90L))
                            t2Acquired.countDown()
                        }
                    }
                    awaitLockWaiter(database) shouldBe true
                    t2Acquired.count shouldBe 1L

                    t1Proceed.countDown()
                    t2Acquired.await(5, SECONDS) shouldBe true
                } finally {
                    executor.shutdownNow()
                }
            }

            it("rolesOf 는 사용자의 각 스페이스 role 을 map 으로 반환하고 매핑 없는 space 는 결과에서 제외한다") {
                transaction(database) {
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(60L),
                            spaceId = SpaceId(100L),
                            userId = UserId(600L),
                            role = SpaceMemberRole.OWNER
                        )
                    )
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(61L),
                            spaceId = SpaceId(101L),
                            userId = UserId(600L),
                            role = SpaceMemberRole.MEMBER
                        )
                    )
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(62L),
                            spaceId = SpaceId(102L),
                            userId = UserId(601L),
                            role = SpaceMemberRole.OWNER
                        )
                    )
                }

                transaction(database) {
                    repository.rolesOf(
                        userId = UserId(600L),
                        spaceIds = setOf(SpaceId(100L), SpaceId(101L), SpaceId(102L))
                    ) shouldBe
                        mapOf(
                            SpaceId(100L) to SpaceMemberRole.OWNER,
                            SpaceId(101L) to SpaceMemberRole.MEMBER
                        )
                }
            }

            it("rolesOf 는 빈 spaceIds 에 대해 emptyMap 반환한다") {
                transaction(database) {
                    repository.rolesOf(UserId(1L), emptyList()) shouldBe emptyMap()
                }
            }

            it("memberCountsOf 는 스페이스별 멤버 수를 반환하고 0 명 스페이스는 결과에서 제외한다") {
                transaction(database) {
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(70L),
                            spaceId = SpaceId(110L),
                            userId = UserId(700L)
                        )
                    )
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(71L),
                            spaceId = SpaceId(110L),
                            userId = UserId(701L)
                        )
                    )
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(72L),
                            spaceId = SpaceId(110L),
                            userId = UserId(702L)
                        )
                    )
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(73L),
                            spaceId = SpaceId(111L),
                            userId = UserId(703L)
                        )
                    )
                }

                transaction(database) {
                    repository.memberCountsOf(
                        setOf(SpaceId(110L), SpaceId(111L), SpaceId(112L))
                    ) shouldBe
                        mapOf(SpaceId(110L) to 3L, SpaceId(111L) to 1L)
                }
            }

            it("memberCountsOf 는 빈 spaceIds 에 대해 emptyMap 반환한다") {
                transaction(database) {
                    repository.memberCountsOf(emptyList()) shouldBe emptyMap()
                }
            }

            it("findBySpaceId 는 joinedAt ASC 순으로 정렬한다") {
                transaction(database) {
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(50L),
                            spaceId = SpaceId(80L),
                            userId = UserId(501L),
                            joinedAt = DUMMY_INSTANT.plusSeconds(60)
                        )
                    )
                    repository.save(
                        basicSpaceMember(
                            id = SpaceMemberId(51L),
                            spaceId = SpaceId(80L),
                            userId = UserId(502L),
                            joinedAt = DUMMY_INSTANT
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findBySpaceId(
                            spaceId = SpaceId(80L),
                            pageRequest = PageRequest(page = 0, size = 10)
                        )
                    result.items.map { it.id } shouldContainExactlyInAnyOrder
                        listOf(SpaceMemberId(51L), SpaceMemberId(50L))
                    result.items.first().id shouldBe SpaceMemberId(51L)
                }
            }
        }
    })

package com.crispinlab.space.adapter.persistence.audit

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.space.domain.audit.AuditChangeSummary
import com.crispinlab.space.domain.audit.SpaceAuditAction
import com.crispinlab.space.domain.audit.SpaceAuditEntryId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicSpaceAuditEntry
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedSpaceAuditRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedSpaceAuditRepository()

        afterEach {
            PostgresTestContext.truncateAll()
        }

        describe("ExposedSpaceAuditRepository") {
            it("save 후 findBy 로 동일 entity 가 복원된다") {
                transaction(database) {
                    repository.save(
                        basicSpaceAuditEntry(
                            id = SpaceAuditEntryId(1L),
                            spaceId = SpaceId(10L),
                            actorUserId = UserId(100L),
                            action = SpaceAuditAction.EDITED,
                            changeSummary =
                                AuditChangeSummary(
                                    """{"name":{"before":"a","after":"b"}}"""
                                )
                        )
                    )
                }

                transaction(database) {
                    val found = repository.findBy(SpaceAuditEntryId(1L))

                    found.shouldNotBeNull()
                    found.id shouldBe SpaceAuditEntryId(1L)
                    found.spaceId shouldBe SpaceId(10L)
                    found.actorUserId shouldBe UserId(100L)
                    found.action shouldBe SpaceAuditAction.EDITED
                    found.changeSummary.json shouldBe """{"name":{"before":"a","after":"b"}}"""
                    found.createdAt shouldBe DUMMY_INSTANT
                }
            }

            it("존재하지 않는 id 는 null 을 반환한다") {
                transaction(database) {
                    repository.findBy(SpaceAuditEntryId(999L)).shouldBeNull()
                }
            }

            it("findBySpaceId 는 해당 space 의 이력을 최신순으로 페이징 반환한다") {
                transaction(database) {
                    repository.save(
                        basicSpaceAuditEntry(
                            id = SpaceAuditEntryId(1L),
                            spaceId = SpaceId(10L),
                            createdAt = DUMMY_INSTANT
                        )
                    )
                    repository.save(
                        basicSpaceAuditEntry(
                            id = SpaceAuditEntryId(2L),
                            spaceId = SpaceId(10L),
                            createdAt = DUMMY_INSTANT.plusSeconds(10)
                        )
                    )
                    repository.save(
                        basicSpaceAuditEntry(
                            id = SpaceAuditEntryId(3L),
                            spaceId = SpaceId(10L),
                            createdAt = DUMMY_INSTANT.plusSeconds(20)
                        )
                    )
                    repository.save(
                        basicSpaceAuditEntry(
                            id = SpaceAuditEntryId(4L),
                            spaceId = SpaceId(99L),
                            createdAt = DUMMY_INSTANT.plusSeconds(30)
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findBySpaceId(
                            spaceId = SpaceId(10L),
                            pageRequest = PageRequest(page = 0, size = 2)
                        )

                    result.items shouldHaveSize 2
                    result.totalElements shouldBe 3L
                    result.totalPages shouldBe 2
                    result.hasNext shouldBe true
                    result.items[0].id shouldBe SpaceAuditEntryId(3L)
                    result.items[1].id shouldBe SpaceAuditEntryId(2L)
                }
            }

            it("findBySpaceId 는 같은 createdAt 이면 id 큰 순으로 정렬한다") {
                transaction(database) {
                    repository.save(
                        basicSpaceAuditEntry(
                            id = SpaceAuditEntryId(11L),
                            spaceId = SpaceId(20L),
                            createdAt = DUMMY_INSTANT
                        )
                    )
                    repository.save(
                        basicSpaceAuditEntry(
                            id = SpaceAuditEntryId(13L),
                            spaceId = SpaceId(20L),
                            createdAt = DUMMY_INSTANT
                        )
                    )
                    repository.save(
                        basicSpaceAuditEntry(
                            id = SpaceAuditEntryId(12L),
                            spaceId = SpaceId(20L),
                            createdAt = DUMMY_INSTANT
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.findBySpaceId(
                            spaceId = SpaceId(20L),
                            pageRequest = PageRequest(page = 0, size = 10)
                        )

                    result.items.map { it.id } shouldBe
                        listOf(
                            SpaceAuditEntryId(13L),
                            SpaceAuditEntryId(12L),
                            SpaceAuditEntryId(11L)
                        )
                }
            }
        }
    })

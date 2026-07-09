package com.crispinlab.space.domain.audit

import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicSpaceAuditEntry
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SpaceAuditEntryTest :
    DescribeSpec({
        describe("생성자") {
            it("전달된 필드를 그대로 노출한다") {
                val entry =
                    basicSpaceAuditEntry(
                        id = SpaceAuditEntryId(1L),
                        spaceId = SpaceId(10L),
                        actorUserId = UserId(100L),
                        action = SpaceAuditAction.EDITED,
                        changeSummary =
                            AuditChangeSummary(
                                """{"name":{"before":"a","after":"b"}}"""
                            ),
                        createdAt = DUMMY_INSTANT
                    )

                entry.id shouldBe SpaceAuditEntryId(1L)
                entry.spaceId shouldBe SpaceId(10L)
                entry.actorUserId shouldBe UserId(100L)
                entry.action shouldBe SpaceAuditAction.EDITED
                entry.changeSummary.json shouldBe """{"name":{"before":"a","after":"b"}}"""
                entry.createdAt shouldBe DUMMY_INSTANT
            }
        }
    })

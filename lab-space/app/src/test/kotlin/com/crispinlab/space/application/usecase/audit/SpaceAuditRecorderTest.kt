package com.crispinlab.space.application.usecase.audit

import com.crispinlab.common.id.IdGenerator
import com.crispinlab.space.application.port.outgoing.audit.SpaceAuditRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.audit.SpaceAuditAction
import com.crispinlab.space.domain.audit.SpaceAuditEntry
import com.crispinlab.space.domain.audit.SpaceAuditEntryId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceSnapshot
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import com.crispinlab.user.domain.user.UserId
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class SpaceAuditRecorderTest :
    DescribeSpec({
        val spaceAuditRepository = mockk<SpaceAuditRepository>()
        val idGenerator = mockk<IdGenerator>()
        val objectMapper: ObjectMapper = jacksonObjectMapper()
        val recorder =
            SpaceAuditRecorder(
                spaceAuditRepository = spaceAuditRepository,
                idGenerator = idGenerator,
                objectMapper = objectMapper
            )

        beforeEach {
            clearMocks(spaceAuditRepository, idGenerator)
            every { idGenerator.next() } returns 999L
            every { spaceAuditRepository.save(any()) } answers { firstArg() }
        }

        describe("recordRegistered") {
            it("REGISTERED 액션과 초기 스냅샷 JSON 을 저장한다") {
                val space =
                    basicSpace(
                        id = SpaceId(10L),
                        name = "새 공간",
                        description = "설명",
                        visibility = SpaceVisibility.INTERNAL
                    )
                val captured = slot<SpaceAuditEntry>()
                every { spaceAuditRepository.save(capture(captured)) } answers { captured.captured }

                recorder.recordRegistered(space, memberViewer())

                captured.captured.id shouldBe SpaceAuditEntryId(999L)
                captured.captured.spaceId shouldBe SpaceId(10L)
                captured.captured.actorUserId shouldBe UserId(100L)
                captured.captured.action shouldBe SpaceAuditAction.REGISTERED
                val parsed = objectMapper.readTree(captured.captured.changeSummary.json)
                parsed["name"].asText() shouldBe "새 공간"
                parsed["description"].asText() shouldBe "설명"
                parsed["visibility"].asText() shouldBe "INTERNAL"
            }
        }

        describe("recordEdited") {
            it("변경된 필드만 diff 로 저장한다") {
                val before =
                    SpaceSnapshot(
                        name = "이전 이름",
                        description = "이전 설명",
                        visibility = SpaceVisibility.INTERNAL
                    )
                val after =
                    basicSpace(
                        id = SpaceId(10L),
                        name = "새 이름",
                        description = "이전 설명",
                        visibility = SpaceVisibility.INTERNAL
                    )
                val captured = slot<SpaceAuditEntry>()
                every { spaceAuditRepository.save(capture(captured)) } answers { captured.captured }

                recorder.recordEdited(
                    spaceId = SpaceId(10L),
                    before = before,
                    after = after,
                    viewer = memberViewer()
                )

                captured.captured.action shouldBe SpaceAuditAction.EDITED
                val parsed = objectMapper.readTree(captured.captured.changeSummary.json)
                parsed["name"]["before"].asText() shouldBe "이전 이름"
                parsed["name"]["after"].asText() shouldBe "새 이름"
                parsed.has("description") shouldBe false
                parsed.has("visibility") shouldBe false
            }

            it("모든 필드가 바뀌면 세 필드 모두 diff 에 담긴다") {
                val before =
                    SpaceSnapshot(
                        name = "이전 이름",
                        description = "이전 설명",
                        visibility = SpaceVisibility.INTERNAL
                    )
                val after =
                    basicSpace(
                        id = SpaceId(10L),
                        name = "새 이름",
                        description = "새 설명",
                        visibility = SpaceVisibility.PUBLIC
                    )
                val captured = slot<SpaceAuditEntry>()
                every { spaceAuditRepository.save(capture(captured)) } answers { captured.captured }

                recorder.recordEdited(
                    spaceId = SpaceId(10L),
                    before = before,
                    after = after,
                    viewer = memberViewer()
                )

                val parsed = objectMapper.readTree(captured.captured.changeSummary.json)
                parsed["name"]["after"].asText() shouldBe "새 이름"
                parsed["description"]["after"].asText() shouldBe "새 설명"
                parsed["visibility"]["before"].asText() shouldBe "INTERNAL"
                parsed["visibility"]["after"].asText() shouldBe "PUBLIC"
            }

            it("변경이 하나도 없으면 audit 을 저장하지 않는다") {
                val space =
                    basicSpace(
                        id = SpaceId(10L),
                        name = "같음",
                        description = "같음",
                        visibility = SpaceVisibility.INTERNAL
                    )

                recorder.recordEdited(
                    spaceId = SpaceId(10L),
                    before = SpaceSnapshot.of(space),
                    after = space,
                    viewer = memberViewer()
                )

                verify(exactly = 0) { spaceAuditRepository.save(any()) }
            }
        }

        describe("recordDeleted") {
            it("DELETED 액션과 마지막 스냅샷 JSON 을 저장한다") {
                val captured = slot<SpaceAuditEntry>()
                every { spaceAuditRepository.save(capture(captured)) } answers { captured.captured }

                recorder.recordDeleted(
                    spaceId = SpaceId(10L),
                    snapshot =
                        SpaceSnapshot(
                            name = "지워질 공간",
                            description = "설명",
                            visibility = SpaceVisibility.PUBLIC
                        ),
                    viewer = memberViewer()
                )

                captured.captured.action shouldBe SpaceAuditAction.DELETED
                val parsed = objectMapper.readTree(captured.captured.changeSummary.json)
                parsed["name"].asText() shouldBe "지워질 공간"
                parsed["description"].asText() shouldBe "설명"
                parsed["visibility"].asText() shouldBe "PUBLIC"
            }
        }
    }) {
    companion object {
        fun memberViewer(
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Viewer.Member = Viewer.Member(userId = userId, isAdmin = isAdmin)
    }
}

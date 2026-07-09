package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.audit.SpaceAuditRecorder
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceSnapshot
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import com.crispinlab.space.testsupport.Fixtures.basicSpaceMember
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class SpaceEditingUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val spaceAuditRecorder = mockk<SpaceAuditRecorder>()
        val useCase =
            SpaceEditingUseCase(
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                spaceAuditRecorder = spaceAuditRecorder,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(spaceRepository, spaceMemberRepository, spaceAuditRecorder)
            every {
                spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
            } returns basicSpaceMember(role = SpaceMemberRole.OWNER)
            justRun {
                spaceAuditRecorder.recordEdited(any(), any(), any(), any())
            }
        }

        describe("스페이스 수정") {
            it("OWNER 가 이름·설명을 모두 변경하면 updatedAt 이 새 값으로 갱신된다") {
                val space = basicSpace(name = "이전 이름", description = "이전 설명")
                every { spaceRepository.findBy(space.id) } returns space
                val saved = slot<Space>()
                every { spaceRepository.save(capture(saved)) } answers { saved.captured }

                val result = useCase.perform(basicRequest(name = "새 이름", description = "새 설명"))

                result.name shouldBe "새 이름"
                result.description shouldBe "새 설명"
                saved.captured.updatedAt shouldNotBe DUMMY_INSTANT
            }

            it("description 만 변경하면 name 은 그대로 유지된다") {
                val space = basicSpace(name = "유지", description = "이전 설명")
                every { spaceRepository.findBy(space.id) } returns space
                val saved = slot<Space>()
                every { spaceRepository.save(capture(saved)) } answers { saved.captured }

                val result = useCase.perform(basicRequest(description = "새 설명"))

                result.name shouldBe "유지"
                result.description shouldBe "새 설명"
            }

            it("스페이스가 없으면 NotFoundException") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(name = "x"))
                }
            }

            it("OWNER 가 아니면 ForbiddenException") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns basicSpaceMember(role = SpaceMemberRole.MEMBER)

                shouldThrow<ForbiddenException> {
                    useCase.perform(basicRequest(name = "x"))
                }
                verify(exactly = 0) { spaceRepository.save(any()) }
            }

            it("멤버가 아니면 ForbiddenException") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns null

                shouldThrow<ForbiddenException> {
                    useCase.perform(basicRequest(name = "x"))
                }
                verify(exactly = 0) { spaceRepository.save(any()) }
            }

            it("ADMIN 은 멤버가 아니어도 수정할 수 있다") {
                val space = basicSpace(name = "이전")
                every { spaceRepository.findBy(space.id) } returns space
                every { spaceRepository.save(any()) } answers { firstArg() }

                useCase.perform(basicRequest(name = "새", isAdmin = true))

                verify(exactly = 1) { spaceRepository.save(any()) }
                verify(exactly = 0) {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                }
            }

            it("수정 성공 시 before 스냅샷과 after 로 EDITED audit 을 기록한다") {
                val space =
                    basicSpace(
                        name = "이전 이름",
                        description = "이전 설명",
                        visibility = SpaceVisibility.INTERNAL
                    )
                every { spaceRepository.findBy(space.id) } returns space
                every { spaceRepository.save(any()) } answers { firstArg() }
                val before = slot<SpaceSnapshot>()
                val after = slot<Space>()
                justRun {
                    spaceAuditRecorder.recordEdited(
                        spaceId = any(),
                        before = capture(before),
                        after = capture(after),
                        viewer = any()
                    )
                }

                useCase.perform(basicRequest(name = "새 이름"))

                before.captured.name shouldBe "이전 이름"
                before.captured.description shouldBe "이전 설명"
                before.captured.visibility shouldBe SpaceVisibility.INTERNAL
                after.captured.name shouldBe "새 이름"
            }

            it("실패 흐름에서는 audit 기록이 일어나지 않는다") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(name = "x"))
                }
                verify(exactly = 0) {
                    spaceAuditRecorder.recordEdited(any(), any(), any(), any())
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            spaceId: String = "1",
            name: String? = null,
            description: String? = null,
            visibility: String? = null,
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                spaceId = spaceId,
                name = name,
                description = description,
                visibility = visibility,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}

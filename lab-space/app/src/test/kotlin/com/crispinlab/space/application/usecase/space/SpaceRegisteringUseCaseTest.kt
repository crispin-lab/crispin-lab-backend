package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.audit.SpaceAuditRecorder
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMember
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class SpaceRegisteringUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val spaceAuditRecorder = mockk<SpaceAuditRecorder>()
        val idGenerator = mockk<IdGenerator>()
        val useCase =
            SpaceRegisteringUseCase(
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                spaceAuditRecorder = spaceAuditRecorder,
                idGenerator = idGenerator,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(spaceRepository, spaceMemberRepository, spaceAuditRecorder, idGenerator)
            every { spaceRepository.save(any()) } answers { firstArg() }
            every { spaceMemberRepository.save(any()) } answers { firstArg() }
            justRun { spaceAuditRecorder.recordRegistered(any(), any()) }
        }

        describe("스페이스 생성") {
            it("새 ID 로 저장하고 호출자를 OWNER 로 자동 등록한다") {
                every { idGenerator.next() } returnsMany listOf(42L, 43L)
                val savedSpace = slot<Space>()
                val savedMember = slot<SpaceMember>()
                every { spaceRepository.save(capture(savedSpace)) } answers { savedSpace.captured }
                every {
                    spaceMemberRepository.save(capture(savedMember))
                } answers { savedMember.captured }

                val result = useCase.perform(basicRequest(userId = UserId(100L)))

                result.spaceId shouldBe SpaceId(42L)
                savedSpace.captured.name shouldBe "팀 위키"
                savedMember.captured.spaceId shouldBe SpaceId(42L)
                savedMember.captured.userId shouldBe UserId(100L)
                savedMember.captured.role shouldBe SpaceMemberRole.OWNER
            }

            it("일반 USER 도 Space 를 생성할 수 있다") {
                every { idGenerator.next() } returnsMany listOf(42L, 43L)

                useCase.perform(basicRequest(isAdmin = false))

                verify(exactly = 1) { spaceRepository.save(any()) }
                verify(exactly = 1) { spaceMemberRepository.save(any()) }
            }

            it("이름이 비어 있으면 IllegalArgumentException 이 발생하고 저장이 일어나지 않는다") {
                every { idGenerator.next() } returns 1L

                shouldThrow<IllegalArgumentException> {
                    useCase.perform(basicRequest(name = "", description = ""))
                }
                verify(exactly = 0) { spaceRepository.save(any()) }
                verify(exactly = 0) { spaceMemberRepository.save(any()) }
                verify(exactly = 0) { spaceAuditRecorder.recordRegistered(any(), any()) }
            }

            it("Space 저장 자체가 실패하면 audit 이 남지 않는다") {
                every { idGenerator.next() } returnsMany listOf(42L, 43L)
                every { spaceRepository.save(any()) } throws RuntimeException("db down")

                shouldThrow<RuntimeException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { spaceAuditRecorder.recordRegistered(any(), any()) }
            }

            it("생성 성공 시 REGISTERED audit 이 저장된 Space·viewer 와 함께 기록된다") {
                every { idGenerator.next() } returnsMany listOf(42L, 43L)
                val savedSpace = slot<Space>()
                every { spaceRepository.save(capture(savedSpace)) } answers { savedSpace.captured }
                val auditedSpace = slot<Space>()
                val auditedViewer = slot<Viewer.Member>()
                justRun {
                    spaceAuditRecorder.recordRegistered(
                        capture(auditedSpace),
                        capture(auditedViewer)
                    )
                }

                useCase.perform(basicRequest(userId = UserId(100L)))

                auditedSpace.captured.id shouldBe SpaceId(42L)
                auditedViewer.captured.userId shouldBe UserId(100L)
                verify(exactly = 1) { spaceAuditRecorder.recordRegistered(any(), any()) }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            name: String = "팀 위키",
            description: String = "공유 공간",
            visibility: String = "INTERNAL",
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                name = name,
                description = description,
                visibility = visibility,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}

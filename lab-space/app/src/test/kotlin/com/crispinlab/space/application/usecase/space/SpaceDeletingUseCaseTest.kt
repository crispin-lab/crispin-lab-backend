package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceDeleting.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.audit.SpaceAuditRecorder
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceSnapshot
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import com.crispinlab.space.testsupport.Fixtures.basicSpaceMember
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

class SpaceDeletingUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val spaceAuditRecorder = mockk<SpaceAuditRecorder>()
        val useCase =
            SpaceDeletingUseCase(
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
            justRun { spaceAuditRecorder.recordDeleted(any(), any(), any()) }
        }

        describe("스페이스 삭제") {
            it("OWNER 가 삭제하면 spaceRepository.delete 호출") {
                val space = basicSpace(id = SpaceId(1L))
                every { spaceRepository.findBy(space.id) } returns space
                justRun { spaceRepository.delete(space.id) }

                useCase.perform(basicRequest(spaceId = space.id.value.toString()))

                verify(exactly = 1) { spaceRepository.delete(space.id) }
            }

            it("스페이스가 없으면 NotFoundException 을 던지고 delete 호출이 발생하지 않는다") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { spaceRepository.delete(any()) }
            }

            it("OWNER 가 아니면 ForbiddenException") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns basicSpaceMember(role = SpaceMemberRole.MEMBER)

                shouldThrow<ForbiddenException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { spaceRepository.delete(any()) }
            }

            it("멤버가 아니면 ForbiddenException") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns null

                shouldThrow<ForbiddenException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { spaceRepository.delete(any()) }
            }

            it("ADMIN 은 멤버가 아니어도 삭제할 수 있다") {
                val space = basicSpace(id = SpaceId(1L))
                every { spaceRepository.findBy(space.id) } returns space
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns null
                justRun { spaceRepository.delete(space.id) }

                useCase.perform(basicRequest(isAdmin = true))

                verify(exactly = 1) { spaceRepository.delete(any()) }
            }

            it("삭제 성공 시 삭제 시점 스냅샷과 함께 DELETED audit 을 기록한다") {
                val space =
                    basicSpace(
                        id = SpaceId(1L),
                        name = "지워질 공간",
                        description = "설명",
                        visibility = SpaceVisibility.PUBLIC
                    )
                every { spaceRepository.findBy(space.id) } returns space
                justRun { spaceRepository.delete(space.id) }
                val snapshot = slot<SpaceSnapshot>()
                justRun {
                    spaceAuditRecorder.recordDeleted(
                        spaceId = any(),
                        snapshot = capture(snapshot),
                        viewer = any()
                    )
                }

                useCase.perform(basicRequest(spaceId = space.id.value.toString()))

                snapshot.captured.name shouldBe "지워질 공간"
                snapshot.captured.description shouldBe "설명"
                snapshot.captured.visibility shouldBe SpaceVisibility.PUBLIC
            }

            it("실패 흐름에서는 audit 기록이 일어나지 않는다") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) {
                    spaceAuditRecorder.recordDeleted(any(), any(), any())
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            spaceId: String = "1",
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                spaceId = spaceId,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}

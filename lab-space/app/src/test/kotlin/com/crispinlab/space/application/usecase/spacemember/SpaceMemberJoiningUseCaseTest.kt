package com.crispinlab.space.application.usecase.spacemember

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberJoining.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMember
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import com.crispinlab.space.testsupport.Fixtures.basicSpaceMember
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class SpaceMemberJoiningUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val idGenerator = mockk<IdGenerator>()
        val useCase =
            SpaceMemberJoiningUseCase(
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                idGenerator = idGenerator,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(spaceRepository, spaceMemberRepository, idGenerator)
            every { spaceRepository.findBy(any()) } returns basicSpace(id = SpaceId(10L))
            every { spaceMemberRepository.save(any()) } answers { firstArg() }
        }

        describe("스페이스 멤버 가입") {
            it("자가 가입 시 MEMBER 로 저장한다") {
                every { idGenerator.next() } returns 42L
                every { spaceMemberRepository.findBySpaceIdAndUserId(any(), any()) } returns null
                val saved = slot<SpaceMember>()
                every { spaceMemberRepository.save(capture(saved)) } answers { saved.captured }

                val result = useCase.perform(basicRequest())

                result.role shouldBe SpaceMemberRole.MEMBER
                saved.captured.userId shouldBe UserId(100L)
                saved.captured.role shouldBe SpaceMemberRole.MEMBER
                saved.captured.spaceId shouldBe SpaceId(10L)
            }

            it("OWNER 가 다른 사용자를 초대하면 명시한 role 로 저장한다") {
                every { idGenerator.next() } returns 42L
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(100L))
                } returns basicSpaceMember(userId = UserId(100L), role = SpaceMemberRole.OWNER)
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(200L))
                } returns null
                val saved = slot<SpaceMember>()
                every { spaceMemberRepository.save(capture(saved)) } answers { saved.captured }

                val result =
                    useCase.perform(
                        basicRequest(targetUserId = "200", role = "VIEWER")
                    )

                result.userId shouldBe UserId(200L)
                result.role shouldBe SpaceMemberRole.VIEWER
                saved.captured.userId shouldBe UserId(200L)
                saved.captured.role shouldBe SpaceMemberRole.VIEWER
            }

            it("일반 MEMBER 가 다른 사용자를 초대하려 하면 ForbiddenException") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(100L))
                } returns basicSpaceMember(userId = UserId(100L), role = SpaceMemberRole.MEMBER)

                shouldThrow<ForbiddenException> {
                    useCase.perform(basicRequest(targetUserId = "200"))
                }
                verify(exactly = 0) { spaceMemberRepository.save(any()) }
            }

            it("ADMIN 은 OWNER 가 아니어도 다른 사용자를 초대할 수 있다") {
                every { idGenerator.next() } returns 42L
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(200L))
                } returns null
                every { spaceMemberRepository.save(any()) } answers { firstArg() }

                useCase.perform(basicRequest(targetUserId = "200", isAdmin = true))

                verify(exactly = 1) { spaceMemberRepository.save(any()) }
            }

            it("이미 가입된 사용자이면 ConflictException 으로 차단한다") {
                every { spaceMemberRepository.findBySpaceIdAndUserId(any(), any()) } returns
                    basicSpaceMember()

                shouldThrow<ConflictException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { spaceMemberRepository.save(any()) }
            }

            it("Space 가 존재하지 않으면 NotFoundException") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { spaceMemberRepository.save(any()) }
            }

            it("자가 가입 시 role 인자를 명시해도 무시되고 MEMBER 로 강제된다") {
                every { idGenerator.next() } returns 42L
                every { spaceMemberRepository.findBySpaceIdAndUserId(any(), any()) } returns null
                val saved = slot<SpaceMember>()
                every { spaceMemberRepository.save(capture(saved)) } answers { saved.captured }

                useCase.perform(basicRequest(role = "OWNER"))

                saved.captured.role shouldBe SpaceMemberRole.MEMBER
            }
        }
    }) {
    companion object {
        fun basicRequest(
            spaceId: String = "10",
            targetUserId: String? = null,
            role: String? = null,
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                spaceId = spaceId,
                targetUserId = targetUserId,
                role = role,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}

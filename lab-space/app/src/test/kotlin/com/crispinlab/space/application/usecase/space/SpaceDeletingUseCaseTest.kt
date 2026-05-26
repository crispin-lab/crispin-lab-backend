package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.space.SpaceDeleting.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import com.crispinlab.space.testsupport.Fixtures.basicSpaceMember
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class SpaceDeletingUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            SpaceDeletingUseCase(
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(spaceRepository, spaceMemberRepository)
            every {
                spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
            } returns basicSpaceMember(role = SpaceMemberRole.OWNER)
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

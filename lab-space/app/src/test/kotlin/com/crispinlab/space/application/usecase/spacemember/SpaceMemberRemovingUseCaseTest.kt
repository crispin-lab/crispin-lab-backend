package com.crispinlab.space.application.usecase.spacemember

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRemoving.Request
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicSpaceMember
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class SpaceMemberRemovingUseCaseTest :
    DescribeSpec({
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            SpaceMemberRemovingUseCase(
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(spaceMemberRepository)
            justRun { spaceMemberRepository.delete(any()) }
        }

        describe("스페이스 멤버 제거") {
            it("본인 탈퇴 시 권한 검증 없이 삭제한다") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(100L))
                } returns
                    basicSpaceMember(
                        id = SpaceMemberId(1L),
                        userId = UserId(100L),
                        role = SpaceMemberRole.MEMBER
                    )

                useCase.perform(basicRequest(targetUserId = "100"))

                verify(exactly = 1) { spaceMemberRepository.delete(SpaceMemberId(1L)) }
            }

            it("OWNER 는 다른 사용자를 추방할 수 있다") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(100L))
                } returns
                    basicSpaceMember(
                        id = SpaceMemberId(1L),
                        userId = UserId(100L),
                        role = SpaceMemberRole.OWNER
                    )
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(200L))
                } returns
                    basicSpaceMember(
                        id = SpaceMemberId(2L),
                        userId = UserId(200L),
                        role = SpaceMemberRole.MEMBER
                    )

                useCase.perform(basicRequest(targetUserId = "200"))

                verify(exactly = 1) { spaceMemberRepository.delete(SpaceMemberId(2L)) }
            }

            it("일반 MEMBER 가 다른 사용자를 추방하려 하면 ForbiddenException") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(100L))
                } returns basicSpaceMember(role = SpaceMemberRole.MEMBER)

                shouldThrow<ForbiddenException> {
                    useCase.perform(basicRequest(targetUserId = "200"))
                }
                verify(exactly = 0) { spaceMemberRepository.delete(any()) }
            }

            it("대상 멤버가 없으면 NotFoundException") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(100L))
                } returns basicSpaceMember(role = SpaceMemberRole.OWNER)
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(200L))
                } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(targetUserId = "200"))
                }
                verify(exactly = 0) { spaceMemberRepository.delete(any()) }
            }

            it("마지막 OWNER 를 제거하려 하면 ConflictException") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(100L))
                } returns
                    basicSpaceMember(
                        id = SpaceMemberId(1L),
                        userId = UserId(100L),
                        role = SpaceMemberRole.OWNER
                    )
                every { spaceMemberRepository.countOwnersBy(SpaceId(10L)) } returns 1L

                shouldThrow<ConflictException> {
                    useCase.perform(basicRequest(targetUserId = "100"))
                }
                verify(exactly = 0) { spaceMemberRepository.delete(any()) }
            }

            it("ADMIN 은 OWNER 가 아니어도 다른 사용자를 추방할 수 있다") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(200L))
                } returns
                    basicSpaceMember(
                        id = SpaceMemberId(2L),
                        userId = UserId(200L),
                        role = SpaceMemberRole.MEMBER
                    )

                useCase.perform(basicRequest(targetUserId = "200", isAdmin = true))

                verify(exactly = 1) { spaceMemberRepository.delete(SpaceMemberId(2L)) }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            spaceId: String = "10",
            targetUserId: String = "200",
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                spaceId = spaceId,
                targetUserId = targetUserId,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}

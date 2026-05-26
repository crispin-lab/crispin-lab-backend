package com.crispinlab.space.application.usecase.spacemember

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRoleChanging.Request
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMember
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.DummyTransactionProvider
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

class SpaceMemberRoleChangingUseCaseTest :
    DescribeSpec({
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            SpaceMemberRoleChangingUseCase(
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(spaceMemberRepository)
            every { spaceMemberRepository.save(any()) } answers { firstArg() }
        }

        describe("스페이스 멤버 역할 변경") {
            it("OWNER 는 다른 멤버의 role 을 변경할 수 있다") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(100L))
                } returns basicSpaceMember(userId = UserId(100L), role = SpaceMemberRole.OWNER)
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(200L))
                } returns
                    basicSpaceMember(
                        id = SpaceMemberId(2L),
                        userId = UserId(200L),
                        role = SpaceMemberRole.MEMBER
                    )
                val saved = slot<SpaceMember>()
                every { spaceMemberRepository.save(capture(saved)) } answers { saved.captured }

                val result =
                    useCase.perform(
                        basicRequest(targetUserId = "200", role = "OWNER")
                    )

                result.role shouldBe SpaceMemberRole.OWNER
                saved.captured.role shouldBe SpaceMemberRole.OWNER
            }

            it("일반 MEMBER 가 role 변경을 시도하면 ForbiddenException") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(100L))
                } returns basicSpaceMember(role = SpaceMemberRole.MEMBER)

                shouldThrow<ForbiddenException> {
                    useCase.perform(basicRequest(targetUserId = "200", role = "OWNER"))
                }
                verify(exactly = 0) { spaceMemberRepository.save(any()) }
            }

            it("대상 멤버가 없으면 NotFoundException") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(100L))
                } returns basicSpaceMember(role = SpaceMemberRole.OWNER)
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(200L))
                } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(targetUserId = "200", role = "MEMBER"))
                }
            }

            it("마지막 OWNER 강등을 application 가드가 차단한다 (race 직렬화는 어댑터 .forUpdate() 책임)") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(100L))
                } returns basicSpaceMember(userId = UserId(100L), role = SpaceMemberRole.OWNER)
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(200L))
                } returns
                    basicSpaceMember(
                        id = SpaceMemberId(2L),
                        userId = UserId(200L),
                        role = SpaceMemberRole.OWNER
                    )
                every { spaceMemberRepository.countOwnersBy(SpaceId(10L)) } returns 1L

                shouldThrow<ConflictException> {
                    useCase.perform(basicRequest(targetUserId = "200", role = "MEMBER"))
                }
                verify(exactly = 0) { spaceMemberRepository.save(any()) }
            }

            it("ADMIN 은 OWNER 가 아니어도 role 을 변경할 수 있다") {
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(200L))
                } returns
                    basicSpaceMember(
                        id = SpaceMemberId(2L),
                        userId = UserId(200L),
                        role = SpaceMemberRole.MEMBER
                    )

                useCase.perform(
                    basicRequest(targetUserId = "200", role = "OWNER", isAdmin = true)
                )

                verify(exactly = 1) { spaceMemberRepository.save(any()) }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            spaceId: String = "10",
            targetUserId: String = "200",
            role: String = "MEMBER",
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

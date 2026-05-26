package com.crispinlab.space.application.usecase.spacemember

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberListing.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
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

class SpaceMemberListingUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            SpaceMemberListingUseCase(
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(spaceRepository, spaceMemberRepository)
        }

        describe("스페이스 멤버 목록 조회") {
            it("PUBLIC 스페이스는 비로그인도 멤버 목록을 본다") {
                every { spaceRepository.findBy(any()) } returns
                    basicSpace(visibility = SpaceVisibility.PUBLIC)
                every { spaceMemberRepository.findBySpaceId(any(), any()) } returns
                    PageResult(
                        items =
                            listOf(
                                basicSpaceMember(
                                    userId = UserId(100L),
                                    role = SpaceMemberRole.OWNER
                                ),
                                basicSpaceMember(
                                    userId = UserId(101L),
                                    role = SpaceMemberRole.MEMBER
                                )
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )

                val result =
                    useCase.perform(basicRequest(viewer = Viewer.Anonymous))

                result.items.size shouldBe 2
                result.totalElements shouldBe 2L
            }

            it("Space 가 없으면 NotFoundException") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
            }

            it("INTERNAL 스페이스는 비로그인에게 NotFoundException") {
                every { spaceRepository.findBy(any()) } returns
                    basicSpace(visibility = SpaceVisibility.INTERNAL)

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(viewer = Viewer.Anonymous))
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            spaceId: String = "10",
            viewer: Viewer = Viewer.Member(userId = UserId(100L), isAdmin = false)
        ): Request =
            Request(
                spaceId = spaceId,
                viewer = viewer
            )
    }
}

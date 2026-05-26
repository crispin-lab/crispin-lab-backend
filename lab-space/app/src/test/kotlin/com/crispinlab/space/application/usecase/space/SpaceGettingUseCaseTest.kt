package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class SpaceGettingUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            SpaceGettingUseCase(
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(spaceRepository, spaceMemberRepository)
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
        }

        describe("스페이스 단건 조회") {
            it("정상적으로 조회한다 — PUBLIC") {
                val space = basicSpace(name = "팀 위키", visibility = SpaceVisibility.PUBLIC)
                every { spaceRepository.findBy(space.id) } returns space

                val result = useCase.perform(basicRequest())

                result.spaceId shouldBe space.id
                result.name shouldBe "팀 위키"
            }

            it("스페이스가 없으면 NotFoundException") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
            }

            it("ID 형식이 올바르지 않으면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(spaceId = "not-a-number")
                }
            }

            it("비로그인 상태에서 PUBLIC 스페이스는 조회 가능하다") {
                val space = basicSpace(visibility = SpaceVisibility.PUBLIC)
                every { spaceRepository.findBy(space.id) } returns space

                val result = useCase.perform(basicRequest(viewer = Viewer.Anonymous))

                result.spaceId shouldBe space.id
                result.visibility shouldBe SpaceVisibility.PUBLIC
            }

            it("비로그인 상태에서 INTERNAL 스페이스는 NotFoundException 으로 응답한다") {
                val space = basicSpace(visibility = SpaceVisibility.INTERNAL)
                every { spaceRepository.findBy(space.id) } returns space

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(viewer = Viewer.Anonymous))
                }
            }

            it("멤버가 아닌 USER 가 INTERNAL 스페이스를 보면 NotFoundException") {
                val space = basicSpace(id = SpaceId(1L), visibility = SpaceVisibility.INTERNAL)
                every { spaceRepository.findBy(space.id) } returns space
                every {
                    spaceMemberRepository.findSpaceIdsByUserId(UserId(100L))
                } returns emptySet()

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
            }

            it("멤버인 USER 는 INTERNAL 스페이스를 조회 가능하다") {
                val space = basicSpace(id = SpaceId(1L), visibility = SpaceVisibility.INTERNAL)
                every { spaceRepository.findBy(space.id) } returns space
                every {
                    spaceMemberRepository.findSpaceIdsByUserId(UserId(100L))
                } returns setOf(SpaceId(1L))

                val result = useCase.perform(basicRequest())

                result.visibility shouldBe SpaceVisibility.INTERNAL
            }

            it("ADMIN 은 멤버가 아니어도 INTERNAL 스페이스를 조회 가능하다") {
                val space = basicSpace(id = SpaceId(1L), visibility = SpaceVisibility.INTERNAL)
                every { spaceRepository.findBy(space.id) } returns space

                val result =
                    useCase.perform(
                        basicRequest(
                            viewer = Viewer.Member(userId = UserId(100L), isAdmin = true)
                        )
                    )

                result.visibility shouldBe SpaceVisibility.INTERNAL
            }
        }
    }) {
    companion object {
        fun basicRequest(
            spaceId: String = "1",
            viewer: Viewer = Viewer.Member(userId = UserId(100L), isAdmin = false)
        ): Request = Request(spaceId = spaceId, viewer = viewer)
    }
}

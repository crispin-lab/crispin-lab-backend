package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceVisibilityScope
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class SpaceListingUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            SpaceListingUseCase(
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(spaceRepository, spaceMemberRepository)
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
        }

        describe("스페이스 목록 조회") {
            it("저장된 스페이스를 Summary 로 매핑해 반환한다") {
                val spaces: List<Space> =
                    listOf(
                        basicSpace(id = SpaceId(2L), name = "최근"),
                        basicSpace(id = SpaceId(1L), name = "이전")
                    )
                every { spaceRepository.findPage(any(), any()) } returns
                    PageResult(
                        items = spaces,
                        page = 2,
                        size = 5,
                        totalElements = 12L
                    )

                val result =
                    useCase.perform(
                        basicRequest(
                            page = 2,
                            size = 5
                        )
                    )

                result.items.map { it.spaceId } shouldBe listOf(SpaceId(2L), SpaceId(1L))
                result.items.map { it.name } shouldBe listOf("최근", "이전")
                result.totalElements shouldBe 12L
                result.page shouldBe 2
                result.size shouldBe 5
                verify {
                    spaceRepository.findPage(
                        withArg {
                            it.page shouldBe 2
                            it.size shouldBe 5
                        },
                        any()
                    )
                }
            }

            it("결과가 비어 있어도 빈 페이지를 반환한다") {
                every { spaceRepository.findPage(any(), any()) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                val result = useCase.perform(basicRequest())

                result.items shouldBe emptyList()
                result.totalElements shouldBe 0L
            }

            it("page 가 음수면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(page = -1)
                }
            }

            it("size 가 허용 범위를 벗어나면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(size = 0)
                }
                shouldThrow<IllegalArgumentException> {
                    basicRequest(size = 201)
                }
            }

            it("비로그인 상태에서는 Anonymous scope 로 조회한다") {
                every { spaceRepository.findPage(any(), any()) } returns
                    PageResult.empty(PageRequest.firstPage())

                useCase.perform(basicRequest(viewer = Viewer.Anonymous))

                verify {
                    spaceRepository.findPage(
                        any(),
                        withArg<SpaceVisibilityScope> {
                            it shouldBe SpaceVisibilityScope.Anonymous
                        }
                    )
                }
            }

            it("로그인 USER 는 Authenticated scope (memberOfSpaceIds 포함) 로 조회한다") {
                every {
                    spaceMemberRepository.findSpaceIdsByUserId(UserId(100L))
                } returns setOf(SpaceId(10L), SpaceId(20L))
                every { spaceRepository.findPage(any(), any()) } returns
                    PageResult.empty(PageRequest.firstPage())

                useCase.perform(basicRequest())

                verify {
                    spaceRepository.findPage(
                        any(),
                        withArg<SpaceVisibilityScope> {
                            it.shouldBeInstanceOf<SpaceVisibilityScope.Authenticated>()
                            it.viewerId shouldBe UserId(100L)
                            it.memberOfSpaceIds shouldBe setOf(SpaceId(10L), SpaceId(20L))
                        }
                    )
                }
            }

            it("ADMIN 은 Privileged scope 로 조회한다") {
                every { spaceRepository.findPage(any(), any()) } returns
                    PageResult.empty(PageRequest.firstPage())

                useCase.perform(
                    basicRequest(
                        viewer = Viewer.Member(userId = UserId(100L), isAdmin = true)
                    )
                )

                verify {
                    spaceRepository.findPage(
                        any(),
                        withArg<SpaceVisibilityScope> {
                            it shouldBe SpaceVisibilityScope.Privileged
                        }
                    )
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            page: Int = 0,
            size: Int = DEFAULT_SIZE,
            viewer: Viewer = Viewer.Member(userId = UserId(100L), isAdmin = false)
        ): Request =
            Request(
                page = page,
                size = size,
                viewer = viewer
            )
    }
}

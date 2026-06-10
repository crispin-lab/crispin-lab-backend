package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing.Request
import com.crispinlab.space.application.port.incoming.tag.TagPopularityListing.Request.Companion.DEFAULT_POPULAR_SIZE
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.port.outgoing.tag.TagPopularitySearchPort
import com.crispinlab.space.application.port.outgoing.tag.TagPopularitySearchPort.TagPopularitySummary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class TagPopularityListingUseCaseTest :
    DescribeSpec({
        val tagPopularitySearchPort = mockk<TagPopularitySearchPort>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            TagPopularityListingUseCase(
                tagPopularitySearchPort = tagPopularitySearchPort,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(tagPopularitySearchPort, spaceMemberRepository)
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
        }

        describe("인기 태그 조회") {
            it("검색 결과를 Summary 로 매핑해 반환한다") {
                every {
                    tagPopularitySearchPort.search(
                        scope = any(),
                        pageRequest = any()
                    )
                } returns
                    PageResult(
                        items =
                            listOf(
                                TagPopularitySummary(name = "kotlin", usageCount = 5L),
                                TagPopularitySummary(name = "spring", usageCount = 3L)
                            ),
                        page = 0,
                        size = DEFAULT_POPULAR_SIZE,
                        totalElements = 2L
                    )

                val result = useCase.perform(basicRequest())

                result.items.map { it.name } shouldBe listOf("kotlin", "spring")
                result.items.map { it.usageCount } shouldBe listOf(5L, 3L)
                result.totalElements shouldBe 2L
                verify {
                    tagPopularitySearchPort.search(
                        scope = match { it is VisibilityScope.Authenticated },
                        pageRequest = any()
                    )
                }
            }

            it("검색 결과가 비어 있어도 빈 페이지를 반환한다") {
                every {
                    tagPopularitySearchPort.search(
                        scope = any(),
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest().pageRequest)

                val result = useCase.perform(basicRequest())

                result.items shouldBe emptyList()
                result.totalElements shouldBe 0L
            }

            it("비로그인 상태에서는 Anonymous scope 로 전달된다") {
                every {
                    tagPopularitySearchPort.search(
                        scope = any(),
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest().pageRequest)

                useCase.perform(basicRequest(viewer = Viewer.Anonymous))

                verify {
                    tagPopularitySearchPort.search(
                        scope = VisibilityScope.Anonymous,
                        pageRequest = any()
                    )
                }
            }

            it("USER 는 Authenticated(viewerId, memberSpaceIds) scope 로 검색한다") {
                every {
                    spaceMemberRepository.findSpaceIdsByUserId(UserId(100L))
                } returns setOf(SpaceId(10L), SpaceId(20L))
                every {
                    tagPopularitySearchPort.search(
                        scope = any(),
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest().pageRequest)

                useCase.perform(
                    basicRequest(
                        viewer = Viewer.Member(userId = UserId(100L), isAdmin = false)
                    )
                )

                verify {
                    tagPopularitySearchPort.search(
                        scope =
                            VisibilityScope.Authenticated(
                                viewerId = UserId(100L),
                                memberOfSpaceIds = setOf(SpaceId(10L), SpaceId(20L))
                            ),
                        pageRequest = any()
                    )
                }
            }

            it("ADMIN 은 Privileged scope 로 검색한다") {
                every {
                    tagPopularitySearchPort.search(
                        scope = any(),
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest().pageRequest)

                useCase.perform(
                    basicRequest(
                        viewer = Viewer.Member(userId = UserId(100L), isAdmin = true)
                    )
                )

                verify {
                    tagPopularitySearchPort.search(
                        scope = VisibilityScope.Privileged,
                        pageRequest = any()
                    )
                }
            }

            it("page 와 size 가 그대로 port 에 전달된다") {
                every {
                    tagPopularitySearchPort.search(
                        scope = any(),
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest(page = 2, size = 10).pageRequest)

                useCase.perform(basicRequest(page = 2, size = 10))

                verify {
                    tagPopularitySearchPort.search(
                        scope = any(),
                        pageRequest =
                            withArg {
                                it.page shouldBe 2
                                it.size shouldBe 10
                            }
                    )
                }
            }
        }

        describe("Request 생성") {
            it("size 가 100 을 넘으면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(size = 101)
                }
            }

            it("size 가 0 이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(size = 0)
                }
            }

            it("page 가 음수면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(page = -1)
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            page: Int = 0,
            size: Int = DEFAULT_POPULAR_SIZE,
            viewer: Viewer = Viewer.Member(userId = UserId(100L), isAdmin = false)
        ): Request =
            Request(
                page = page,
                size = size,
                viewer = viewer
            )
    }
}

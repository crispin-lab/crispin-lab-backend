package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository.SortDirection
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository.SortOption
import com.crispinlab.space.application.port.outgoing.space.SpaceVisibilityScope
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant

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
            it("저장된 스페이스를 Summary 로 매핑하고 lastActivityAt 을 노출한다") {
                val activityLater = DUMMY_INSTANT.plusSeconds(3600)
                every { spaceRepository.findPage(any(), any(), any(), any(), any()) } returns
                    PageResult(
                        items =
                            listOf(
                                summaryOf(
                                    spaceId = 2L,
                                    name = "최근",
                                    lastActivityAt = activityLater
                                ),
                                summaryOf(spaceId = 1L, name = "이전", lastActivityAt = DUMMY_INSTANT)
                            ),
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
                result.items.map { it.lastActivityAt } shouldBe listOf(activityLater, DUMMY_INSTANT)
                result.totalElements shouldBe 12L
                result.page shouldBe 2
                result.size shouldBe 5
            }

            it("결과가 비어 있어도 빈 페이지를 반환한다") {
                every { spaceRepository.findPage(any(), any(), any(), any(), any()) } returns
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

            it("keyword / sort / direction 을 도메인 port 로 그대로 forward 한다") {
                every { spaceRepository.findPage(any(), any(), any(), any(), any()) } returns
                    PageResult.empty(PageRequest.firstPage())

                useCase.perform(
                    basicRequest(
                        keyword = "  위키  ",
                        sort = "NAME",
                        direction = "ASC"
                    )
                )

                verify {
                    spaceRepository.findPage(
                        pageRequest = any(),
                        scope = any(),
                        keyword = "위키",
                        sort = SortOption.NAME,
                        direction = SortDirection.ASC
                    )
                }
            }

            it("sort 미지정 시 default 는 LAST_ACTIVITY_AT DESC 로 forward 된다") {
                every { spaceRepository.findPage(any(), any(), any(), any(), any()) } returns
                    PageResult.empty(PageRequest.firstPage())

                useCase.perform(basicRequest())

                verify {
                    spaceRepository.findPage(
                        pageRequest = any(),
                        scope = any(),
                        keyword = null,
                        sort = SortOption.LAST_ACTIVITY_AT,
                        direction = SortDirection.DESC
                    )
                }
            }

            it("sort=NAME 은 미지정 direction 이면 자연 default (ASC) 를 사용한다") {
                every { spaceRepository.findPage(any(), any(), any(), any(), any()) } returns
                    PageResult.empty(PageRequest.firstPage())

                useCase.perform(basicRequest(sort = "NAME"))

                verify {
                    spaceRepository.findPage(
                        pageRequest = any(),
                        scope = any(),
                        keyword = null,
                        sort = SortOption.NAME,
                        direction = SortDirection.ASC
                    )
                }
            }

            it("빈 문자열 sort/direction 은 미지정과 동일하게 default 로 fallback 된다") {
                every { spaceRepository.findPage(any(), any(), any(), any(), any()) } returns
                    PageResult.empty(PageRequest.firstPage())

                useCase.perform(basicRequest(sort = "", direction = "   "))

                verify {
                    spaceRepository.findPage(
                        pageRequest = any(),
                        scope = any(),
                        keyword = null,
                        sort = SortOption.LAST_ACTIVITY_AT,
                        direction = SortDirection.DESC
                    )
                }
            }

            it("keyword 가 공백뿐이면 null 로 정규화된다") {
                every { spaceRepository.findPage(any(), any(), any(), any(), any()) } returns
                    PageResult.empty(PageRequest.firstPage())

                useCase.perform(basicRequest(keyword = "   "))

                verify {
                    spaceRepository.findPage(
                        pageRequest = any(),
                        scope = any(),
                        keyword = null,
                        sort = any(),
                        direction = any()
                    )
                }
            }

            it("지원하지 않는 sort 값은 Request 생성에서 IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(sort = "SIZE")
                }
            }

            it("지원하지 않는 direction 값은 Request 생성에서 IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(direction = "SIDEWAYS")
                }
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
                every { spaceRepository.findPage(any(), any(), any(), any(), any()) } returns
                    PageResult.empty(PageRequest.firstPage())

                useCase.perform(basicRequest(viewer = Viewer.Anonymous))

                verify {
                    spaceRepository.findPage(
                        pageRequest = any(),
                        scope =
                            withArg<SpaceVisibilityScope> {
                                it shouldBe SpaceVisibilityScope.Anonymous
                            },
                        keyword = any(),
                        sort = any(),
                        direction = any()
                    )
                }
            }

            it("로그인 USER 는 Authenticated scope (memberOfSpaceIds 포함) 로 조회한다") {
                every {
                    spaceMemberRepository.findSpaceIdsByUserId(UserId(100L))
                } returns setOf(SpaceId(10L), SpaceId(20L))
                every { spaceRepository.findPage(any(), any(), any(), any(), any()) } returns
                    PageResult.empty(PageRequest.firstPage())

                useCase.perform(basicRequest())

                verify {
                    spaceRepository.findPage(
                        pageRequest = any(),
                        scope =
                            withArg<SpaceVisibilityScope> {
                                it.shouldBeInstanceOf<SpaceVisibilityScope.Authenticated>()
                                it.viewerId shouldBe UserId(100L)
                                it.memberOfSpaceIds shouldBe setOf(SpaceId(10L), SpaceId(20L))
                            },
                        keyword = any(),
                        sort = any(),
                        direction = any()
                    )
                }
            }

            it("ADMIN 은 Privileged scope 로 조회한다") {
                every { spaceRepository.findPage(any(), any(), any(), any(), any()) } returns
                    PageResult.empty(PageRequest.firstPage())

                useCase.perform(
                    basicRequest(
                        viewer = Viewer.Member(userId = UserId(100L), isAdmin = true)
                    )
                )

                verify {
                    spaceRepository.findPage(
                        pageRequest = any(),
                        scope =
                            withArg<SpaceVisibilityScope> {
                                it shouldBe SpaceVisibilityScope.Privileged
                            },
                        keyword = any(),
                        sort = any(),
                        direction = any()
                    )
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            keyword: String? = null,
            sort: String? = null,
            direction: String? = null,
            page: Int = 0,
            size: Int = DEFAULT_SIZE,
            viewer: Viewer = Viewer.Member(userId = UserId(100L), isAdmin = false)
        ): Request =
            Request(
                keyword = keyword,
                sort = sort,
                direction = direction,
                page = page,
                size = size,
                viewer = viewer
            )

        private fun summaryOf(
            spaceId: Long,
            name: String = "스페이스",
            description: String = "설명",
            visibility: SpaceVisibility = SpaceVisibility.PUBLIC,
            createdAt: Instant = DUMMY_INSTANT,
            updatedAt: Instant = createdAt,
            lastActivityAt: Instant = updatedAt
        ): SpaceRepository.Summary =
            SpaceRepository.Summary(
                spaceId = SpaceId(spaceId),
                name = name,
                description = description,
                visibility = visibility,
                lastActivityAt = lastActivityAt,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
    }
}

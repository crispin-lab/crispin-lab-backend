package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageSearching.Request
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.PageSummary
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.SortOption
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PageSearchingUseCaseTest :
    DescribeSpec({
        val pageSearchPort = mockk<PageSearchPort>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val userHandleQuery = mockk<UserHandleQuery>()
        val useCase =
            PageSearchingUseCase(
                pageSearchPort = pageSearchPort,
                spaceMemberRepository = spaceMemberRepository,
                userHandleQuery = userHandleQuery,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(pageSearchPort, spaceMemberRepository, userHandleQuery)
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
            every { userHandleQuery.handlesOf(any()) } returns
                mapOf(
                    UserId(100L) to Handle("test_user"),
                    UserId(200L) to Handle("other_user")
                )
        }

        describe("페이지 검색") {
            it("검색 결과를 Summary 로 매핑해 반환한다") {
                val summaries: List<PageSummary> =
                    listOf(
                        PageSummary(
                            id = PageId(2L),
                            spaceId = SpaceId(10L),
                            parentPageId = PageId(1L),
                            authorId = UserId(100L),
                            title = "오늘의 회고",
                            visibility = Visibility.PUBLIC,
                            displayOrder = 1,
                            updatedAt = DUMMY_INSTANT
                        ),
                        PageSummary(
                            id = PageId(1L),
                            spaceId = SpaceId(10L),
                            parentPageId = null,
                            authorId = UserId(200L),
                            title = "어제의 회고",
                            visibility = Visibility.INTERNAL,
                            displayOrder = 0,
                            updatedAt = DUMMY_INSTANT
                        )
                    )
                every {
                    pageSearchPort.search(
                        keyword = "회고",
                        spaceId = SpaceId(10L),
                        tagIds = listOf(TagId(100L), TagId(200L)),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                } returns
                    PageResult(
                        items = summaries,
                        page = 0,
                        size = 20,
                        totalElements = 2L
                    )

                val result =
                    useCase.perform(
                        basicRequest(
                            keyword = "회고",
                            spaceId = "10",
                            tagIds = listOf("100", "200")
                        )
                    )

                result.items.map { it.pageId } shouldBe listOf(PageId(2L), PageId(1L))
                result.items.map { it.spaceId } shouldBe listOf(SpaceId(10L), SpaceId(10L))
                result.items.map { it.parentPageId } shouldBe listOf(PageId(1L), null)
                result.items.map { it.authorId } shouldBe listOf(UserId(100L), UserId(200L))
                result.items.map { it.authorHandle } shouldBe listOf("test_user", "other_user")
                result.items.map { it.title } shouldBe listOf("오늘의 회고", "어제의 회고")
                result.items.map { it.visibility } shouldBe
                    listOf(Visibility.PUBLIC, Visibility.INTERNAL)
                result.items.map { it.displayOrder } shouldBe listOf(1, 0)
                result.totalElements shouldBe 2L
                verify {
                    pageSearchPort.search(
                        keyword = "회고",
                        spaceId = SpaceId(10L),
                        tagIds = listOf(TagId(100L), TagId(200L)),
                        sort = any(),
                        scope = any(),
                        pageRequest =
                            withArg<PageRequest> {
                                it.page shouldBe 0
                                it.size shouldBe 20
                            }
                    )
                }
            }

            it("키워드만 공백/빈 문자열이면 null 로 정규화해 전달한다") {
                every {
                    pageSearchPort.search(
                        keyword = null,
                        spaceId = null,
                        tagIds = emptyList(),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest().pageRequest)

                useCase.perform(basicRequest(keyword = "   "))

                verify {
                    pageSearchPort.search(
                        keyword = null,
                        spaceId = null,
                        tagIds = emptyList(),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                }
            }

            it("검색 결과가 비어 있어도 빈 페이지를 반환한다") {
                every {
                    pageSearchPort.search(
                        keyword = null,
                        spaceId = null,
                        tagIds = emptyList(),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest().pageRequest)

                val result = useCase.perform(basicRequest())

                result.items shouldBe emptyList()
                result.totalElements shouldBe 0L
            }

            it("sort 가 지정되면 SortOption 으로 변환되어 port 에 전달된다") {
                every {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest().pageRequest)

                useCase.perform(basicRequest(sort = "CREATED_AT"))

                verify {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        sort = SortOption.CREATED_AT,
                        scope = any(),
                        pageRequest = any()
                    )
                }
            }

            it("sort 미지정 시 SortOption.UPDATED_AT 가 default 로 전달된다") {
                every {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest().pageRequest)

                useCase.perform(basicRequest(sort = null))

                verify {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        sort = SortOption.UPDATED_AT,
                        scope = any(),
                        pageRequest = any()
                    )
                }
            }

            it("비로그인 상태에서는 Anonymous scope 로 전달된다") {
                every {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest().pageRequest)

                useCase.perform(basicRequest(viewer = Viewer.Anonymous))

                verify {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        sort = any(),
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
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        sort = any(),
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
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        sort = any(),
                        scope =
                            VisibilityScope.Authenticated(
                                viewerId = UserId(100L),
                                memberOfSpaceIds = setOf(SpaceId(10L), SpaceId(20L))
                            ),
                        pageRequest = any()
                    )
                }
            }

            it("동일 결과 페이지의 distinct authorIds 에 대해 handlesOf 를 정확히 1회 호출한다") {
                val summaries: List<PageSummary> =
                    listOf(
                        PageSummary(
                            id = PageId(1L),
                            spaceId = SpaceId(10L),
                            parentPageId = null,
                            authorId = UserId(100L),
                            title = "a",
                            visibility = Visibility.PUBLIC,
                            displayOrder = 0,
                            updatedAt = DUMMY_INSTANT
                        ),
                        PageSummary(
                            id = PageId(2L),
                            spaceId = SpaceId(10L),
                            parentPageId = null,
                            authorId = UserId(100L),
                            title = "b",
                            visibility = Visibility.PUBLIC,
                            displayOrder = 1,
                            updatedAt = DUMMY_INSTANT
                        ),
                        PageSummary(
                            id = PageId(3L),
                            spaceId = SpaceId(10L),
                            parentPageId = null,
                            authorId = UserId(200L),
                            title = "c",
                            visibility = Visibility.PUBLIC,
                            displayOrder = 2,
                            updatedAt = DUMMY_INSTANT
                        )
                    )
                every {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                } returns
                    PageResult(
                        items = summaries,
                        page = 0,
                        size = 20,
                        totalElements = 3L
                    )

                useCase.perform(basicRequest())

                verify(exactly = 1) {
                    userHandleQuery.handlesOf(setOf(UserId(100L), UserId(200L)))
                }
            }

            it("handle 조회 결과에 없는 author 는 authorHandle 이 빈 문자열로 응답한다") {
                val summaries: List<PageSummary> =
                    listOf(
                        PageSummary(
                            id = PageId(1L),
                            spaceId = SpaceId(10L),
                            parentPageId = null,
                            authorId = UserId(999L),
                            title = "삭제된 사용자가 쓴 글",
                            visibility = Visibility.PUBLIC,
                            displayOrder = 0,
                            updatedAt = DUMMY_INSTANT
                        )
                    )
                every {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                } returns
                    PageResult(
                        items = summaries,
                        page = 0,
                        size = 20,
                        totalElements = 1L
                    )
                every { userHandleQuery.handlesOf(any()) } returns emptyMap()

                val result = useCase.perform(basicRequest())

                result.items.single().authorId shouldBe UserId(999L)
                result.items.single().authorHandle shouldBe ""
            }

            it("ADMIN 은 Privileged scope 로 검색한다") {
                every {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        sort = any(),
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
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        sort = any(),
                        scope = VisibilityScope.Privileged,
                        pageRequest = any()
                    )
                }
            }
        }

        describe("Request 생성") {
            it("지원하지 않는 sort 값이면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(sort = "UNKNOWN")
                }
            }

            it("spaceId 형식이 숫자가 아니면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(spaceId = "abc")
                }
            }

            it("tagIds 중 하나라도 숫자가 아니면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(tagIds = listOf("1", "xx"))
                }
            }

            it("page 가 음수면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(page = -1)
                }
            }

            it("size 가 허용 범위를 벗어나면 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(size = 0)
                }
                shouldThrow<IllegalArgumentException> {
                    basicRequest(size = 201)
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            keyword: String? = null,
            spaceId: String? = null,
            tagIds: List<String> = emptyList(),
            sort: String? = null,
            page: Int = 0,
            size: Int = DEFAULT_SIZE,
            viewer: Viewer = Viewer.Member(userId = UserId(100L), isAdmin = false)
        ): Request =
            Request(
                keyword = keyword,
                spaceId = spaceId,
                tagIds = tagIds,
                sort = sort,
                page = page,
                size = size,
                viewer = viewer
            )
    }
}

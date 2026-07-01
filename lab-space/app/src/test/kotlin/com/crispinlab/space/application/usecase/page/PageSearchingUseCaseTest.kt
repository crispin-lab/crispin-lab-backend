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
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
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
        val tagRepository = mockk<TagRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            PageSearchingUseCase(
                pageSearchPort = pageSearchPort,
                tagRepository = tagRepository,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(pageSearchPort, tagRepository, spaceMemberRepository)
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
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
                        tagIdsAnyOf = emptyList(),
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
                        tagIdsAnyOf = emptyList(),
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
                        tagIdsAnyOf = emptyList(),
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
                        tagIdsAnyOf = emptyList(),
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
                        tagIdsAnyOf = emptyList(),
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
                        tagIdsAnyOf = any(),
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
                        tagIdsAnyOf = any(),
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
                        tagIdsAnyOf = any(),
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
                        tagIdsAnyOf = any(),
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
                        tagIdsAnyOf = any(),
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
                        tagIdsAnyOf = any(),
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
                        tagIdsAnyOf = any(),
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
                        tagIdsAnyOf = any(),
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

            it("tagName 이 cross-space 로 해석돼 tagIdsAnyOf 로 port 에 전달된다") {
                every { tagRepository.findIdsByName("kotlin") } returns
                    listOf(TagId(500L), TagId(600L))
                every {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        tagIdsAnyOf = any(),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest().pageRequest)

                useCase.perform(basicRequest(tagName = "kotlin"))

                verify {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = emptyList(),
                        tagIdsAnyOf = listOf(TagId(500L), TagId(600L)),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                }
            }

            it("tagName 이 어느 tag 와도 매치되지 않으면 port 호출 없이 빈 결과를 반환한다") {
                every { tagRepository.findIdsByName("존재하지않음") } returns emptyList()

                val result = useCase.perform(basicRequest(tagName = "존재하지않음"))

                result.items shouldBe emptyList()
                result.totalElements shouldBe 0L
                verify(exactly = 1) { tagRepository.findIdsByName("존재하지않음") }
                verify(exactly = 0) {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        tagIdsAnyOf = any(),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                }
            }

            it("tagName 공백/빈 문자열은 null 로 정규화돼 TagRepository 를 호출하지 않는다") {
                every {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        tagIdsAnyOf = any(),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest().pageRequest)

                useCase.perform(basicRequest(tagName = "   "))

                verify(exactly = 0) { tagRepository.findIdsByName(any()) }
                verify {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        tagIdsAnyOf = emptyList(),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                }
            }

            it("tag (AND) 와 tagName (OR) 이 함께 오면 두 필터가 모두 port 로 전달된다") {
                every { tagRepository.findIdsByName("kotlin") } returns
                    listOf(TagId(500L), TagId(600L))
                every {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        tagIdsAnyOf = any(),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest().pageRequest)

                useCase.perform(
                    basicRequest(tagIds = listOf("100", "200"), tagName = "kotlin")
                )

                verify {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = listOf(TagId(100L), TagId(200L)),
                        tagIdsAnyOf = listOf(TagId(500L), TagId(600L)),
                        sort = any(),
                        scope = any(),
                        pageRequest = any()
                    )
                }
            }

            it("ADMIN 은 Privileged scope 로 검색한다") {
                every {
                    pageSearchPort.search(
                        keyword = any(),
                        spaceId = any(),
                        tagIds = any(),
                        tagIdsAnyOf = any(),
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
                        tagIdsAnyOf = any(),
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
            tagName: String? = null,
            sort: String? = null,
            page: Int = 0,
            size: Int = DEFAULT_SIZE,
            viewer: Viewer = Viewer.Member(userId = UserId(100L), isAdmin = false)
        ): Request =
            Request(
                keyword = keyword,
                spaceId = spaceId,
                tagIds = tagIds,
                tagName = tagName,
                sort = sort,
                page = page,
                size = size,
                viewer = viewer
            )
    }
}

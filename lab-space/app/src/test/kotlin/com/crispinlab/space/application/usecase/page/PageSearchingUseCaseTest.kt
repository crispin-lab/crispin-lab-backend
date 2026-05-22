package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.page.PageSearching.Request
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.PageSummary
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.DummyTransactionProvider
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
        val useCase = PageSearchingUseCase(pageSearchPort, DummyTransactionProvider())

        beforeEach {
            clearMocks(pageSearchPort)
        }

        describe("페이지 검색") {
            it("검색 결과를 Summary 로 매핑해 반환한다") {
                val summaries: List<PageSummary> =
                    listOf(
                        PageSummary(
                            id = PageId(2L),
                            spaceId = SpaceId(10L),
                            title = "오늘의 회고",
                            updatedAt = DUMMY_INSTANT
                        ),
                        PageSummary(
                            id = PageId(1L),
                            spaceId = SpaceId(10L),
                            title = "어제의 회고",
                            updatedAt = DUMMY_INSTANT
                        )
                    )
                every {
                    pageSearchPort.search(
                        keyword = "회고",
                        spaceId = SpaceId(10L),
                        tagIds = listOf(TagId(100L), TagId(200L)),
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
                result.items.map { it.title } shouldBe listOf("오늘의 회고", "어제의 회고")
                result.totalElements shouldBe 2L
                verify {
                    pageSearchPort.search(
                        keyword = "회고",
                        spaceId = SpaceId(10L),
                        tagIds = listOf(TagId(100L), TagId(200L)),
                        pageRequest =
                            withArg {
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
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest().pageRequest)

                useCase.perform(basicRequest(keyword = "   "))

                verify {
                    pageSearchPort.search(
                        keyword = null,
                        spaceId = null,
                        tagIds = emptyList(),
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
                        pageRequest = any()
                    )
                } returns PageResult.empty(basicRequest().pageRequest)

                val result = useCase.perform(basicRequest())

                result.items shouldBe emptyList()
                result.totalElements shouldBe 0L
            }

            it("spaceId 형식이 숫자가 아니면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(spaceId = "abc")
                }
            }

            it("tagIds 중 하나라도 숫자가 아니면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(tagIds = listOf("1", "xx"))
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
        }
    }) {
    companion object {
        fun basicRequest(
            keyword: String? = null,
            spaceId: String? = null,
            tagIds: List<String> = emptyList(),
            page: Int = 0,
            size: Int = DEFAULT_SIZE,
            currentUserId: UserId = UserId(100L)
        ): Request =
            Request(
                keyword = keyword,
                spaceId = spaceId,
                tagIds = tagIds,
                page = page,
                size = size,
                currentUserId = currentUserId
            )
    }
}

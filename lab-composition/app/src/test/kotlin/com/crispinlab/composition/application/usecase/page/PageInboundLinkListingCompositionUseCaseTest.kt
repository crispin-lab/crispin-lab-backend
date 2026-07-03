package com.crispinlab.composition.application.usecase.page

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.RecordingTransactionProvider
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.page.PageInboundLinkListingComposition.Request
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing
import com.crispinlab.space.application.port.incoming.page.PageInboundLinkListing.Summary
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class PageInboundLinkListingCompositionUseCaseTest :
    DescribeSpec({
        val pageInboundLinkListing = mockk<PageInboundLinkListing>()
        val userHandleLookup = mockk<UserHandleLookup>()

        fun useCaseWith(transactionProvider: TransactionProvider = RecordingTransactionProvider()) =
            PageInboundLinkListingCompositionUseCase(
                pageInboundLinkListing = pageInboundLinkListing,
                userHandleLookup = userHandleLookup,
                transactionProvider = transactionProvider
            )

        beforeEach { clearMocks(pageInboundLinkListing, userHandleLookup) }

        describe("페이지 인바운드 링크 목록 조립") {
            it("도메인 결과에 authorHandle 을 붙여 Result 로 반환한다") {
                every { pageInboundLinkListing.perform(any()) } returns
                    PageResult(
                        items = listOf(domainSummary(pageId = 11L, authorId = 100L, title = "이전")),
                        page = 0,
                        size = 20,
                        totalElements = 1L
                    )
                every { userHandleLookup.handlesOf(any()) } returns mapOf(UserId(100L) to "alice")

                val result = useCaseWith().perform(basicRequest())

                result.items.single().authorHandle shouldBe "alice"
            }

            it("distinct authorIds 로 handlesOf 를 batch 1회 호출한다") {
                every { pageInboundLinkListing.perform(any()) } returns
                    PageResult(
                        items =
                            listOf(
                                domainSummary(pageId = 11L, authorId = 100L),
                                domainSummary(pageId = 12L, authorId = 100L),
                                domainSummary(pageId = 13L, authorId = 200L)
                            ),
                        page = 0,
                        size = 20,
                        totalElements = 3L
                    )
                every { userHandleLookup.handlesOf(any()) } returns
                    mapOf(UserId(100L) to "alice", UserId(200L) to "bob")

                useCaseWith().perform(basicRequest())

                verify(exactly = 1) {
                    userHandleLookup.handlesOf(setOf(UserId(100L), UserId(200L)))
                }
            }

            it("lookup miss 이면 authorHandle 을 빈 문자열로 채운다") {
                every { pageInboundLinkListing.perform(any()) } returns
                    PageResult(
                        items = listOf(domainSummary(pageId = 11L, authorId = 999L)),
                        page = 0,
                        size = 20,
                        totalElements = 1L
                    )
                every { userHandleLookup.handlesOf(any()) } returns emptyMap()

                val result = useCaseWith().perform(basicRequest())

                result.items.single().authorHandle shouldBe ""
            }

            it("Request 를 도메인 UseCase Request 로 그대로 넘긴다") {
                val requestSlot = slot<PageInboundLinkListing.Request>()
                every { pageInboundLinkListing.perform(capture(requestSlot)) } returns
                    PageResult(items = emptyList(), page = 0, size = 20, totalElements = 0L)
                every { userHandleLookup.handlesOf(any()) } returns emptyMap()

                useCaseWith().perform(
                    Request(pageId = "42", page = 1, size = 50, viewer = Viewer.Anonymous)
                )

                requestSlot.captured.pageId shouldBe PageId(42L)
                requestSlot.captured.pageRequest.page shouldBe 1
                requestSlot.captured.pageRequest.size shouldBe 50
                requestSlot.captured.viewer shouldBe Viewer.Anonymous
            }

            it("perform 진입에서 readOnly 트랜잭션으로 감싸고 도메인 호출·lookup 모두 tx 블록 안에서 실행한다 (LAB-156 회귀 방지)") {
                val transactionProvider = RecordingTransactionProvider()
                every { pageInboundLinkListing.perform(any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    PageResult(items = emptyList(), page = 0, size = 20, totalElements = 0L)
                }
                every { userHandleLookup.handlesOf(any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    emptyMap()
                }

                useCaseWith(transactionProvider).perform(basicRequest())

                transactionProvider.readOnlyInvocations shouldBe listOf(true)
            }

            it("pageId 형식이 숫자가 아니면 IllegalArgumentException 을 그대로 올린다") {
                shouldThrow<IllegalArgumentException> {
                    useCaseWith().perform(basicRequest(pageId = "not-a-number"))
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(pageId: String = "42"): Request =
            Request(
                pageId = pageId,
                page = 0,
                size = 20,
                viewer = Viewer.Anonymous
            )

        fun domainSummary(
            pageId: Long,
            authorId: Long,
            title: String = "테스트"
        ): Summary =
            Summary(
                pageId = PageId(pageId),
                spaceId = SpaceId(10L),
                parentPageId = null,
                authorId = UserId(authorId),
                title = title,
                visibility = Visibility.PUBLIC,
                displayOrder = 0,
                updatedAt = DUMMY_INSTANT
            )
    }
}

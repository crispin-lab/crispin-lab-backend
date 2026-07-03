package com.crispinlab.composition.application.usecase.page

import com.crispinlab.common.transaction.RecordingTransactionProvider
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.page.PageGettingComposition.Request
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.space.application.port.incoming.page.PageGetting
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

class PageGettingCompositionUseCaseTest :
    DescribeSpec({
        val pageGetting = mockk<PageGetting>()
        val userHandleLookup = mockk<UserHandleLookup>()

        fun useCaseWith(transactionProvider: TransactionProvider = RecordingTransactionProvider()) =
            PageGettingCompositionUseCase(
                pageGetting = pageGetting,
                userHandleLookup = userHandleLookup,
                transactionProvider = transactionProvider
            )

        beforeEach { clearMocks(pageGetting, userHandleLookup) }

        describe("페이지 단건 조립") {
            it("도메인 Result 에 authorHandle 을 붙여 Result 로 반환한다") {
                every { pageGetting.perform(any()) } returns
                    domainResult(authorId = 100L, title = "회고")
                every { userHandleLookup.handlesOf(setOf(UserId(100L))) } returns
                    mapOf(UserId(100L) to "alice")

                val result = useCaseWith().perform(basicRequest())

                result.authorHandle shouldBe "alice"
                result.title shouldBe "회고"
            }

            it("lookup miss 이면 authorHandle 을 빈 문자열로 채운다") {
                every { pageGetting.perform(any()) } returns domainResult(authorId = 999L)
                every { userHandleLookup.handlesOf(any()) } returns emptyMap()

                val result = useCaseWith().perform(basicRequest())

                result.authorHandle shouldBe ""
            }

            it("도메인 Result 의 AncestorSummary 를 그대로 매핑한다") {
                every { pageGetting.perform(any()) } returns
                    domainResult(
                        authorId = 100L,
                        ancestors =
                            listOf(
                                PageGetting.Result.AncestorSummary(
                                    pageId = PageId(1L),
                                    title = "루트"
                                ),
                                PageGetting.Result.AncestorSummary(
                                    pageId = PageId(2L),
                                    title = "부모"
                                )
                            )
                    )
                every { userHandleLookup.handlesOf(any()) } returns
                    mapOf(UserId(100L) to "alice")

                val result = useCaseWith().perform(basicRequest())

                result.ancestors.map { it.pageId to it.title } shouldBe
                    listOf(PageId(1L) to "루트", PageId(2L) to "부모")
            }

            it("Request 를 도메인 UseCase Request 로 그대로 넘긴다") {
                val requestSlot = slot<PageGetting.Request>()
                every { pageGetting.perform(capture(requestSlot)) } returns
                    domainResult(authorId = 100L)
                every { userHandleLookup.handlesOf(any()) } returns
                    mapOf(UserId(100L) to "alice")

                useCaseWith().perform(Request(pageId = "42", viewer = Viewer.Anonymous))

                requestSlot.captured.pageId shouldBe PageId(42L)
                requestSlot.captured.viewer shouldBe Viewer.Anonymous
            }

            it("perform 진입에서 readOnly 트랜잭션으로 감싸고 도메인 호출·lookup 모두 tx 블록 안에서 실행한다 (LAB-156 회귀 방지)") {
                val transactionProvider = RecordingTransactionProvider()
                every { pageGetting.perform(any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    domainResult(authorId = 100L)
                }
                every { userHandleLookup.handlesOf(any()) } answers {
                    transactionProvider.inTransaction shouldBe true
                    mapOf(UserId(100L) to "alice")
                }

                useCaseWith(transactionProvider).perform(basicRequest())

                transactionProvider.readOnlyInvocations shouldBe listOf(true)
            }

            it("pageId 형식이 숫자가 아니면 IllegalArgumentException 을 그대로 올린다") {
                shouldThrow<IllegalArgumentException> {
                    useCaseWith().perform(
                        Request(pageId = "not-a-number", viewer = Viewer.Anonymous)
                    )
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(): Request = Request(pageId = "1", viewer = Viewer.Anonymous)

        fun domainResult(
            authorId: Long,
            title: String = "테스트",
            ancestors: List<PageGetting.Result.AncestorSummary> = emptyList()
        ): PageGetting.Result =
            PageGetting.Result(
                pageId = PageId(1L),
                spaceId = SpaceId(10L),
                parentPageId = null,
                authorId = UserId(authorId),
                title = title,
                content = "본문",
                visibility = Visibility.PUBLIC,
                currentVersion = 1,
                displayOrder = 0,
                canEdit = false,
                canComment = false,
                createdAt = DUMMY_INSTANT,
                updatedAt = DUMMY_INSTANT,
                ancestors = ancestors
            )
    }
}

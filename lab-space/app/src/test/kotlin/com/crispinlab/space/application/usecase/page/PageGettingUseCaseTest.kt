package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class PageGettingUseCaseTest :
    DescribeSpec({
        val pageRepository = mockk<PageRepository>()
        val useCase = PageGettingUseCase(pageRepository, DummyTransactionProvider())

        beforeEach {
            clearMocks(pageRepository)
        }

        describe("페이지 단건 조회") {
            it("정상적으로 조회한다") {
                val page = basicPage(title = "오늘의 회고")
                every { pageRepository.findBy(page.id) } returns page

                val result = useCase.perform(basicRequest(pageId = page.id.value.toString()))

                result.pageId shouldBe page.id
                result.title shouldBe "오늘의 회고"
                result.visibility shouldBe "DRAFT"
            }

            it("페이지가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
            }

            it("ID 형식이 올바르지 않으면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(pageId = "not-a-number")
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "1",
            currentUserId: UserId = UserId(100L)
        ): Request = Request(pageId = pageId, currentUserId = currentUserId)
    }
}

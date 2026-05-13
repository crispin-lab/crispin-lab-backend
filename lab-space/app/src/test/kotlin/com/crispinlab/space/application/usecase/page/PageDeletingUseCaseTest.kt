package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.page.PageDeleting.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicPage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class PageDeletingUseCaseTest :
    DescribeSpec({
        val pageRepository = mockk<PageRepository>()
        val useCase = PageDeletingUseCase(pageRepository, DummyTransactionProvider())

        beforeEach {
            clearMocks(pageRepository)
        }

        describe("페이지 삭제") {
            it("Page 만 삭제한다 (Revision/Link 는 이력으로 보존)") {
                val page = basicPage()
                every { pageRepository.findBy(page.id) } returns page
                justRun { pageRepository.delete(page.id) }

                useCase.perform(basicRequest(pageId = page.id.value.toString()))

                verify(exactly = 1) { pageRepository.delete(page.id) }
            }

            it("페이지가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { pageRepository.delete(any()) }
            }

            it("작성자가 아니면 NotFoundException 으로 응답한다") {
                val page = basicPage(authorId = UserId(200L))
                every { pageRepository.findBy(page.id) } returns page

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(currentUserId = UserId(100L)))
                }
                verify(exactly = 0) { pageRepository.delete(any()) }
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

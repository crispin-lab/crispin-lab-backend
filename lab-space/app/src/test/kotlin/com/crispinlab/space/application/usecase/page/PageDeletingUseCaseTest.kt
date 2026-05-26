package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.page.PageDeleting.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.Fixtures.basicSpaceMember
import com.crispinlab.user.domain.user.UserId
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
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            PageDeletingUseCase(
                pageRepository = pageRepository,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(pageRepository, spaceMemberRepository)
            every {
                spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
            } returns basicSpaceMember(role = SpaceMemberRole.MEMBER)
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
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { pageRepository.delete(any()) }
            }

            it("작성자더라도 멤버에서 추방됐으면 ForbiddenException") {
                val page = basicPage(authorId = UserId(100L))
                every { pageRepository.findBy(page.id) } returns page
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns null

                shouldThrow<ForbiddenException> {
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { pageRepository.delete(any()) }
            }

            it("ADMIN 은 작성자가 아니어도, 멤버가 아니어도 삭제 가능하다") {
                val page = basicPage(authorId = UserId(200L))
                every { pageRepository.findBy(page.id) } returns page
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns null
                justRun { pageRepository.delete(page.id) }

                useCase.perform(
                    basicRequest(
                        pageId = page.id.value.toString(),
                        userId = UserId(100L),
                        isAdmin = true
                    )
                )

                verify(exactly = 1) { pageRepository.delete(page.id) }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "1",
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                pageId = pageId,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}

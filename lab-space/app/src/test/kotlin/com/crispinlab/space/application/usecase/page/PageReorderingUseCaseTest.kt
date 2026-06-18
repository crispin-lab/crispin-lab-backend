package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageReordering.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.Fixtures.basicSpaceMember
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class PageReorderingUseCaseTest :
    DescribeSpec({
        val pageRepository = mockk<PageRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            PageReorderingUseCase(
                pageRepository = pageRepository,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(pageRepository, spaceMemberRepository)
            every {
                spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
            } returns basicSpaceMember(role = SpaceMemberRole.MEMBER)
            every { pageRepository.save(any()) } answers { firstArg() }
        }

        describe("페이지 순서 변경") {
            it("displayOrder 가 entity 에 반영되어 저장된다") {
                val page = basicPage(displayOrder = 0)
                every { pageRepository.findBy(page.id) } returns page
                val savedPage = slot<Page>()
                every { pageRepository.save(capture(savedPage)) } answers { savedPage.captured }

                useCase.perform(
                    basicRequest(
                        pageId = page.id.value.toString(),
                        displayOrder = 9
                    )
                )

                savedPage.captured.displayOrder shouldBe 9
                verify(exactly = 1) { pageRepository.save(any()) }
            }

            it("작성자가 아니면 NotFoundException (IDOR 통합)") {
                val page = basicPage(authorId = UserId(200L))
                every { pageRepository.findBy(page.id) } returns page

                shouldThrow<NotFoundException> {
                    useCase.perform(
                        basicRequest(
                            pageId = page.id.value.toString(),
                            userId = UserId(100L)
                        )
                    )
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("ADMIN 은 작성자가 아니어도 순서를 변경할 수 있다") {
                val page = basicPage(authorId = UserId(200L), displayOrder = 0)
                every { pageRepository.findBy(page.id) } returns page
                val savedPage = slot<Page>()
                every { pageRepository.save(capture(savedPage)) } answers { savedPage.captured }

                useCase.perform(
                    basicRequest(
                        pageId = page.id.value.toString(),
                        displayOrder = 2,
                        userId = UserId(100L),
                        isAdmin = true
                    )
                )

                savedPage.captured.displayOrder shouldBe 2
            }

            it("작성자라도 멤버에서 추방됐으면 ForbiddenException") {
                val page = basicPage(authorId = UserId(100L))
                every { pageRepository.findBy(page.id) } returns page
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                } returns null

                shouldThrow<ForbiddenException> {
                    useCase.perform(
                        basicRequest(
                            pageId = page.id.value.toString(),
                            userId = UserId(100L)
                        )
                    )
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("페이지가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }

            it("displayOrder 가 음수면 IllegalArgumentException (entity invariant)") {
                val page = basicPage()
                every { pageRepository.findBy(page.id) } returns page

                shouldThrow<IllegalArgumentException> {
                    useCase.perform(
                        basicRequest(
                            pageId = page.id.value.toString(),
                            displayOrder = -1
                        )
                    )
                }
                verify(exactly = 0) { pageRepository.save(any()) }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "1",
            displayOrder: Int = 0,
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                pageId = pageId,
                displayOrder = displayOrder,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}

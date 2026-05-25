package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.tag.PageTagDetaching.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class PageTagDetachingUseCaseTest :
    DescribeSpec({
        val tagRepository = mockk<TagRepository>()
        val pageRepository = mockk<PageRepository>()
        val useCase =
            PageTagDetachingUseCase(
                tagRepository = tagRepository,
                pageRepository = pageRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(tagRepository, pageRepository)
            every { pageRepository.findBy(any()) } returns
                basicPage(spaceId = SpaceId(10L), authorId = UserId(100L))
            every { tagRepository.detach(any(), any()) } just runs
        }

        describe("페이지에서 태그 매핑 제거") {
            it("페이지 작성자가 호출하면 detach 한다") {
                useCase.perform(basicRequest(pageId = "100", tagId = "200"))

                verify(exactly = 1) { tagRepository.detach(PageId(100L), TagId(200L)) }
            }

            it("ADMIN 은 페이지 작성자가 아니어도 detach 한다") {
                useCase.perform(
                    basicRequest(
                        pageId = "100",
                        tagId = "200",
                        userId = UserId(200L),
                        isAdmin = true
                    )
                )

                verify(exactly = 1) { tagRepository.detach(PageId(100L), TagId(200L)) }
            }

            it("다른 사용자가 호출하면 NotFoundException (IDOR)") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(spaceId = SpaceId(10L), authorId = UserId(200L))

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { tagRepository.detach(any(), any()) }
            }

            it("Page 가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { tagRepository.detach(any(), any()) }
            }

            it("권한 통과 후 매핑이 없어도 예외 없이 성공한다 (멱등)") {
                useCase.perform(basicRequest())

                verify(exactly = 1) { tagRepository.detach(any(), any()) }
            }

            it("pageId 형식이 올바르지 않으면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(pageId = "not-a-number")
                }
            }

            it("tagId 형식이 올바르지 않으면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(tagId = "not-a-number")
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            pageId: String = "100",
            tagId: String = "200",
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                pageId = pageId,
                tagId = tagId,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}

package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.tag.PageTagAttaching.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.PageTag
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.Fixtures.basicTag
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify

class PageTagAttachingUseCaseTest :
    DescribeSpec({
        val tagRepository = mockk<TagRepository>()
        val pageRepository = mockk<PageRepository>()
        val useCase =
            PageTagAttachingUseCase(
                tagRepository = tagRepository,
                pageRepository = pageRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(tagRepository, pageRepository)
            every { pageRepository.findBy(any()) } returns
                basicPage(spaceId = SpaceId(10L), authorId = UserId(100L))
            every { tagRepository.findBy(any()) } returns basicTag(spaceId = SpaceId(10L))
            every { tagRepository.attach(any()) } just runs
        }

        describe("페이지에 태그 매핑") {
            it("페이지 작성자가 호출하면 attach 한다") {
                val attached = slot<PageTag>()
                every { tagRepository.attach(capture(attached)) } just runs

                useCase.perform(basicRequest(pageId = "100", tagId = "200"))

                attached.captured.pageId shouldBe PageId(100L)
                attached.captured.tagId shouldBe TagId(200L)
            }

            it("ADMIN 은 페이지 작성자가 아니어도 attach 한다") {
                val attached = slot<PageTag>()
                every { tagRepository.attach(capture(attached)) } just runs

                useCase.perform(
                    basicRequest(
                        pageId = "100",
                        tagId = "200",
                        userId = UserId(200L),
                        isAdmin = true
                    )
                )

                attached.captured.pageId shouldBe PageId(100L)
            }

            it("다른 사용자가 호출하면 NotFoundException (IDOR)") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(spaceId = SpaceId(10L), authorId = UserId(200L))

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { tagRepository.attach(any()) }
            }

            it("Page 가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { tagRepository.attach(any()) }
            }

            it("Tag 가 없으면 NotFoundException") {
                every { tagRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { tagRepository.attach(any()) }
            }

            it("Tag 의 space 와 Page 의 space 가 다르면 NotFoundException (정보 노출 방지)") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(spaceId = SpaceId(10L), authorId = UserId(100L))
                every { tagRepository.findBy(any()) } returns basicTag(spaceId = SpaceId(99L))

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { tagRepository.attach(any()) }
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

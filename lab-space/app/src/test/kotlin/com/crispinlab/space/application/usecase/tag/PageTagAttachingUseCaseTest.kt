package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.tag.PageTagAttaching.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.PageTag
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicPage
import com.crispinlab.space.testsupport.Fixtures.basicTag
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
            every { pageRepository.findBy(any()) } returns basicPage(spaceId = SpaceId(10L))
            every { tagRepository.findBy(any()) } returns basicTag(spaceId = SpaceId(10L))
            every { tagRepository.attach(any()) } just runs
        }

        describe("페이지에 태그 매핑") {
            it("Page · Tag 가 모두 존재하고 같은 space 면 attach 한다") {
                val attached = slot<PageTag>()
                every { tagRepository.attach(capture(attached)) } just runs

                useCase.perform(basicRequest(pageId = "100", tagId = "200"))

                attached.captured.pageId shouldBe PageId(100L)
                attached.captured.tagId shouldBe TagId(200L)
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
                every { pageRepository.findBy(any()) } returns basicPage(spaceId = SpaceId(10L))
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
            currentUserId: UserId = UserId(1L)
        ): Request =
            Request(
                pageId = pageId,
                tagId = tagId,
                currentUserId = currentUserId
            )
    }
}

package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.tag.PageTagListing.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.Tag
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
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class PageTagListingUseCaseTest :
    DescribeSpec({
        val tagRepository = mockk<TagRepository>()
        val pageRepository = mockk<PageRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val useCase =
            PageTagListingUseCase(
                tagRepository = tagRepository,
                pageRepository = pageRepository,
                spaceMemberRepository = spaceMemberRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(tagRepository, pageRepository, spaceMemberRepository)
            every { pageRepository.findBy(any()) } returns basicPage()
            every { spaceMemberRepository.findSpaceIdsByUserId(any()) } returns emptySet()
        }

        describe("페이지 태그 목록 조회") {
            it("Page 에 매핑된 태그를 Summary 로 매핑해 반환한다") {
                val tags: List<Tag> =
                    listOf(
                        basicTag(id = TagId(1L), spaceId = SpaceId(10L), name = "kotlin"),
                        basicTag(id = TagId(2L), spaceId = SpaceId(10L), name = "spring")
                    )
                val capturedPageId = slot<PageId>()
                val capturedPageRequest = slot<PageRequest>()
                every {
                    tagRepository.findTagsByPageId(
                        capture(capturedPageId),
                        capture(capturedPageRequest)
                    )
                } returns
                    PageResult(
                        items = tags,
                        page = 1,
                        size = 5,
                        totalElements = 7L
                    )

                val result =
                    useCase.perform(
                        basicRequest(
                            pageId = "10",
                            page = 1,
                            size = 5
                        )
                    )

                result.items.map { it.tagId } shouldBe listOf(TagId(1L), TagId(2L))
                result.items.map { it.name } shouldBe listOf("kotlin", "spring")
                result.items.map { it.spaceId } shouldBe listOf(SpaceId(10L), SpaceId(10L))
                result.totalElements shouldBe 7L
                result.page shouldBe 1
                result.size shouldBe 5
                capturedPageId.captured.value shouldBe 10L
                capturedPageRequest.captured.page shouldBe 1
                capturedPageRequest.captured.size shouldBe 5
            }

            it("결과가 비어 있어도 빈 페이지를 반환한다") {
                every { tagRepository.findTagsByPageId(any(), any()) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                val result = useCase.perform(basicRequest())

                result.items shouldBe emptyList()
                result.totalElements shouldBe 0L
            }

            it("Page 가 없으면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { tagRepository.findTagsByPageId(any(), any()) }
            }

            it("다른 사용자의 DRAFT 페이지는 NotFoundException") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(authorId = UserId(999L), visibility = Visibility.DRAFT)

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { tagRepository.findTagsByPageId(any(), any()) }
            }

            it("ADMIN 은 다른 사용자의 DRAFT 페이지의 태그도 조회할 수 있다") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(authorId = UserId(999L), visibility = Visibility.DRAFT)
                every { tagRepository.findTagsByPageId(any(), any()) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                useCase.perform(basicRequest(isAdmin = true))

                verify(exactly = 1) { tagRepository.findTagsByPageId(any(), any()) }
            }

            it("INTERNAL 페이지는 Space 멤버가 아니면 NotFoundException") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(spaceId = SpaceId(10L), visibility = Visibility.INTERNAL)
                every {
                    spaceMemberRepository.findSpaceIdsByUserId(any())
                } returns emptySet()

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { tagRepository.findTagsByPageId(any(), any()) }
            }

            it("INTERNAL 페이지는 Space 멤버면 조회할 수 있다") {
                every { pageRepository.findBy(any()) } returns
                    basicPage(spaceId = SpaceId(10L), visibility = Visibility.INTERNAL)
                every {
                    spaceMemberRepository.findSpaceIdsByUserId(any())
                } returns setOf(SpaceId(10L))
                every { tagRepository.findTagsByPageId(any(), any()) } returns
                    PageResult(
                        items = emptyList(),
                        page = 0,
                        size = 20,
                        totalElements = 0L
                    )

                useCase.perform(basicRequest())

                verify(exactly = 1) { tagRepository.findTagsByPageId(any(), any()) }
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
            pageId: String = "10",
            page: Int = 0,
            size: Int = DEFAULT_SIZE,
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                pageId = pageId,
                page = page,
                size = size,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}

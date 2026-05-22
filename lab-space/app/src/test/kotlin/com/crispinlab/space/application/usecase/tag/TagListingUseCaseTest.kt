package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageRequest.Companion.DEFAULT_SIZE
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.application.port.incoming.tag.TagListing.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.Tag
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicSpace
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

class TagListingUseCaseTest :
    DescribeSpec({
        val tagRepository = mockk<TagRepository>()
        val spaceRepository = mockk<SpaceRepository>()
        val useCase =
            TagListingUseCase(
                tagRepository = tagRepository,
                spaceRepository = spaceRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(tagRepository, spaceRepository)
            every { spaceRepository.findBy(any()) } returns basicSpace()
        }

        describe("스페이스 태그 목록 조회") {
            it("Space 의 태그를 Summary 로 매핑해 반환한다") {
                val tags: List<Tag> =
                    listOf(
                        basicTag(id = TagId(1L), spaceId = SpaceId(10L), name = "kotlin"),
                        basicTag(id = TagId(2L), spaceId = SpaceId(10L), name = "spring")
                    )
                val capturedSpaceId = slot<SpaceId>()
                val capturedPageRequest = slot<PageRequest>()
                every {
                    tagRepository.findBySpaceId(
                        capture(capturedSpaceId),
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
                            spaceId = "10",
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
                capturedSpaceId.captured.value shouldBe 10L
                capturedPageRequest.captured.page shouldBe 1
                capturedPageRequest.captured.size shouldBe 5
            }

            it("결과가 비어 있어도 빈 페이지를 반환한다") {
                every { tagRepository.findBySpaceId(any(), any()) } returns
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

            it("Space 가 없으면 NotFoundException") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { tagRepository.findBySpaceId(any(), any()) }
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
            spaceId: String = "10",
            page: Int = 0,
            size: Int = DEFAULT_SIZE,
            currentUserId: UserId = UserId(100L)
        ): Request =
            Request(
                spaceId = spaceId,
                page = page,
                size = size,
                currentUserId = currentUserId
            )
    }
}

package com.crispinlab.space.application.usecase.tag

import com.crispinlab.space.application.port.incoming.tag.TagDeleting.Request
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.tag.TagId
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class TagDeletingUseCaseTest :
    DescribeSpec({
        val tagRepository = mockk<TagRepository>()
        val useCase =
            TagDeletingUseCase(
                tagRepository = tagRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(tagRepository)
            every { tagRepository.delete(any()) } just runs
        }

        describe("태그 삭제") {
            it("delete 를 호출한다") {
                useCase.perform(basicRequest(tagId = "42"))

                verify(exactly = 1) { tagRepository.delete(TagId(42L)) }
            }

            it("Tag 가 없어도 멱등 성공 — delete 를 그대로 호출한다") {
                useCase.perform(basicRequest(tagId = "9999"))

                verify(exactly = 1) { tagRepository.delete(TagId(9999L)) }
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
            tagId: String = "1",
            currentUserId: UserId = UserId(100L)
        ): Request =
            Request(
                tagId = tagId,
                currentUserId = currentUserId
            )
    }
}

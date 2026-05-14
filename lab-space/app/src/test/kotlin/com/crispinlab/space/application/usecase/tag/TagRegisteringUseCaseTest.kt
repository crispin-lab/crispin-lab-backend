package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.space.application.port.incoming.tag.TagRegistering.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.tag.Tag
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class TagRegisteringUseCaseTest :
    DescribeSpec({
        val tagRepository = mockk<TagRepository>()
        val spaceRepository = mockk<SpaceRepository>()
        val idGenerator = mockk<IdGenerator>()
        val useCase =
            TagRegisteringUseCase(
                tagRepository = tagRepository,
                spaceRepository = spaceRepository,
                idGenerator = idGenerator,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(tagRepository, spaceRepository, idGenerator)
            every { spaceRepository.findBy(any()) } returns basicSpace()
            every { tagRepository.existsByNameAndSpaceId(any(), any()) } returns false
            every { tagRepository.save(any()) } answers { firstArg() }
        }

        describe("태그 등록") {
            it("Space 가 존재하고 이름 중복이 없으면 Tag 를 저장하고 tagId 를 반환한다") {
                every { idGenerator.next() } returns 42L
                val saved = slot<Tag>()
                every { tagRepository.save(capture(saved)) } answers { saved.captured }

                val result =
                    useCase.perform(
                        basicRequest(
                            spaceId = "10",
                            name = "kotlin"
                        )
                    )

                result.tagId shouldBe "42"
                saved.captured.spaceId shouldBe SpaceId(10L)
                saved.captured.name shouldBe "kotlin"
            }

            it("Space 가 없으면 NotFoundException") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { tagRepository.save(any()) }
            }

            it("같은 space 에 같은 name 이 이미 있으면 ConflictException") {
                every { tagRepository.existsByNameAndSpaceId(any(), any()) } returns true

                shouldThrow<ConflictException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { tagRepository.save(any()) }
            }

            it("이름 형식이 잘못되면 entity 생성에서 실패한다") {
                every { idGenerator.next() } returns 1L

                shouldThrow<IllegalArgumentException> {
                    useCase.perform(basicRequest(name = "invalid name with space"))
                }
                verify(exactly = 0) { tagRepository.save(any()) }
            }

            it("spaceId 형식이 올바르지 않으면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(spaceId = "not-a-number")
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            spaceId: String = "10",
            name: String = "kotlin",
            currentUserId: UserId = UserId(100L)
        ): Request =
            Request(
                spaceId = spaceId,
                name = name,
                currentUserId = currentUserId
            )
    }
}

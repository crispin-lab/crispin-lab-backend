package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

class SpaceEditingUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val useCase =
            SpaceEditingUseCase(
                spaceRepository = spaceRepository,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(spaceRepository)
        }

        describe("스페이스 수정") {
            it("이름·설명을 모두 변경하면 updatedAt 이 새 값으로 갱신된다") {
                val space = basicSpace(name = "이전 이름", description = "이전 설명")
                every { spaceRepository.findBy(space.id) } returns space
                val saved = slot<Space>()
                every { spaceRepository.save(capture(saved)) } answers { saved.captured }

                val result = useCase.perform(basicRequest(name = "새 이름", description = "새 설명"))

                result.name shouldBe "새 이름"
                result.description shouldBe "새 설명"
                saved.captured.updatedAt shouldNotBe DUMMY_INSTANT
            }

            it("description 만 변경하면 name 은 그대로 유지된다") {
                val space = basicSpace(name = "유지", description = "이전 설명")
                every { spaceRepository.findBy(space.id) } returns space
                val saved = slot<Space>()
                every { spaceRepository.save(capture(saved)) } answers { saved.captured }

                val result = useCase.perform(basicRequest(description = "새 설명"))

                result.name shouldBe "유지"
                result.description shouldBe "새 설명"
            }

            it("스페이스가 없으면 NotFoundException") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(name = "x"))
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            spaceId: String = "1",
            name: String? = null,
            description: String? = null,
            currentUserId: UserId = UserId(100L)
        ): Request =
            Request(
                spaceId = spaceId,
                name = name,
                description = description,
                currentUserId = currentUserId
            )
    }
}

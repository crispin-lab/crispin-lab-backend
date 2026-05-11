package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.id.IdGenerator
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class SpaceRegisteringUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val idGenerator = mockk<IdGenerator>()
        val useCase =
            SpaceRegisteringUseCase(
                spaceRepository = spaceRepository,
                idGenerator = idGenerator,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(spaceRepository, idGenerator)
        }

        describe("스페이스 생성") {
            it("새 ID 로 저장하고 생성된 spaceId 를 반환한다") {
                every { idGenerator.next() } returns 42L
                val saved = slot<Space>()
                every { spaceRepository.save(capture(saved)) } answers { saved.captured }

                val result = useCase.perform(basicRequest())

                result.spaceId shouldBe "42"
                saved.captured.name shouldBe "팀 위키"
                saved.captured.description shouldBe "공유 공간"
                verify(exactly = 1) { spaceRepository.save(any()) }
            }

            it("이름이 비어 있으면 IllegalArgumentException") {
                every { idGenerator.next() } returns 1L

                shouldThrow<IllegalArgumentException> {
                    useCase.perform(basicRequest(name = "", description = ""))
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            name: String = "팀 위키",
            description: String = "공유 공간",
            currentUserId: UserId = UserId(100L)
        ): Request = Request(name = name, description = description, currentUserId = currentUserId)
    }
}

package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.user.UserId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class SpaceGettingUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val useCase = SpaceGettingUseCase(spaceRepository, DummyTransactionProvider())

        beforeEach {
            clearMocks(spaceRepository)
        }

        describe("스페이스 단건 조회") {
            it("정상적으로 조회한다") {
                val space = basicSpace(name = "팀 위키")
                every { spaceRepository.findBy(space.id) } returns space

                val result = useCase.perform(basicRequest())

                result.spaceId shouldBe "1"
                result.name shouldBe "팀 위키"
            }

            it("스페이스가 없으면 NotFoundException") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
            }

            it("ID 형식이 올바르지 않으면 Request 생성에서 실패한다") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(spaceId = "not-a-number")
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            spaceId: String = "1",
            currentUserId: UserId = UserId(100L)
        ): Request = Request(spaceId = spaceId, currentUserId = currentUserId)
    }
}

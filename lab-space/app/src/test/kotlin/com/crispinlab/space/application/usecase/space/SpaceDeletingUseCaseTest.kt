package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.space.SpaceDeleting.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class SpaceDeletingUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val useCase = SpaceDeletingUseCase(spaceRepository, DummyTransactionProvider())

        beforeEach {
            clearMocks(spaceRepository)
        }

        describe("스페이스 삭제") {
            it("존재하면 삭제한다") {
                val space = basicSpace()
                every { spaceRepository.findBy(space.id) } returns space
                justRun { spaceRepository.delete(space.id) }

                useCase.perform(Request(spaceId = "1"))

                verify(exactly = 1) { spaceRepository.delete(SpaceId(1L)) }
            }

            it("스페이스가 없으면 NotFoundException 을 던지고 delete 호출이 발생하지 않는다") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(Request(spaceId = "1"))
                }
                verify(exactly = 0) { spaceRepository.delete(any()) }
            }
        }
    })

package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.time.Clock
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant

class SpaceEditingUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val clock = mockk<Clock>()
        val useCase =
            SpaceEditingUseCase(
                spaceRepository = spaceRepository,
                clock = clock,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(spaceRepository, clock)
        }

        describe("스페이스 수정") {
            it("이름·설명을 변경하고 변경 시점으로 updatedAt 이 갱신된다") {
                val occurredAt = Instant.parse("2026-02-02T00:00:00Z")
                val space = basicSpace(name = "이전 이름", description = "이전 설명")
                every { spaceRepository.findBy(space.id) } returns space
                every { clock.now() } returns occurredAt
                val saved = slot<Space>()
                every { spaceRepository.save(capture(saved)) } answers { saved.captured }

                val result =
                    useCase.perform(
                        Request(spaceId = "1", name = "새 이름", description = "새 설명")
                    )

                result.name shouldBe "새 이름"
                result.description shouldBe "새 설명"
                result.updatedAt shouldBe occurredAt
                saved.captured.updatedAt shouldBe occurredAt
            }

            it("스페이스가 없으면 NotFoundException") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(Request(spaceId = "1", name = "x"))
                }
            }
        }
    })

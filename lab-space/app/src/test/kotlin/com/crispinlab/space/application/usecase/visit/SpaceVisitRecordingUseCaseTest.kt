package com.crispinlab.space.application.usecase.visit

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.space.application.port.incoming.visit.SpaceVisitRecording.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.port.outgoing.visit.SpaceVisitRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.visit.SpaceVisit
import com.crispinlab.space.domain.visit.SpaceVisitId
import com.crispinlab.space.testsupport.Fixtures.basicSpace
import com.crispinlab.space.testsupport.Fixtures.basicSpaceMember
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant

class SpaceVisitRecordingUseCaseTest :
    DescribeSpec({
        val spaceRepository = mockk<SpaceRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val spaceVisitRepository = mockk<SpaceVisitRepository>()
        val idGenerator = mockk<IdGenerator>()
        val useCase =
            SpaceVisitRecordingUseCase(
                spaceRepository = spaceRepository,
                spaceMemberRepository = spaceMemberRepository,
                spaceVisitRepository = spaceVisitRepository,
                idGenerator = idGenerator,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(
                spaceRepository,
                spaceMemberRepository,
                spaceVisitRepository,
                idGenerator
            )
            every { spaceRepository.findBy(any()) } returns
                basicSpace(id = SpaceId(10L), visibility = SpaceVisibility.PUBLIC)
            every { spaceMemberRepository.findBySpaceIdAndUserId(any(), any()) } returns null
            every { spaceVisitRepository.save(any()) } returns Unit
        }

        describe("스페이스 방문 기록") {
            it("PUBLIC 스페이스면 현재 시각으로 SpaceVisit 을 upsert 한다") {
                every { idGenerator.next() } returns 42L
                val saved = slot<SpaceVisit>()
                every { spaceVisitRepository.save(capture(saved)) } returns Unit

                val before = Instant.now()
                useCase.perform(basicRequest())
                val after = Instant.now()

                saved.captured.id shouldBe SpaceVisitId(42L)
                saved.captured.userId shouldBe UserId(100L)
                saved.captured.spaceId shouldBe SpaceId(10L)
                val captured = saved.captured.lastVisitedAt.toEpochMilli()
                captured shouldBeGreaterThanOrEqual before.toEpochMilli()
                captured shouldBeLessThanOrEqual after.toEpochMilli()
            }

            it("존재하지 않는 space 는 NotFoundException 을 던지고 save 하지 않는다") {
                every { spaceRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { spaceVisitRepository.save(any()) }
            }

            it("INTERNAL 스페이스는 멤버가 아닌 viewer 에게 NotFoundException 으로 통합 응답한다") {
                every { spaceRepository.findBy(any()) } returns
                    basicSpace(id = SpaceId(10L), visibility = SpaceVisibility.INTERNAL)
                every { spaceMemberRepository.findBySpaceIdAndUserId(any(), any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { spaceVisitRepository.save(any()) }
            }

            it("INTERNAL 스페이스의 멤버는 방문 기록에 성공한다") {
                every { spaceRepository.findBy(any()) } returns
                    basicSpace(id = SpaceId(10L), visibility = SpaceVisibility.INTERNAL)
                every {
                    spaceMemberRepository.findBySpaceIdAndUserId(SpaceId(10L), UserId(100L))
                } returns basicSpaceMember(spaceId = SpaceId(10L), userId = UserId(100L))
                every { idGenerator.next() } returns 43L

                useCase.perform(basicRequest())

                verify(exactly = 1) { spaceVisitRepository.save(any()) }
            }

            it("ADMIN 은 멤버 조회 없이 INTERNAL 스페이스에도 방문 기록에 성공한다") {
                every { spaceRepository.findBy(any()) } returns
                    basicSpace(id = SpaceId(10L), visibility = SpaceVisibility.INTERNAL)
                every { idGenerator.next() } returns 44L

                useCase.perform(basicRequest(isAdmin = true))

                verify(exactly = 1) { spaceVisitRepository.save(any()) }
                verify(exactly = 0) {
                    spaceMemberRepository.findBySpaceIdAndUserId(any(), any())
                }
            }

            it("입력 spaceId 가 숫자가 아니면 Request 생성 단계에서 IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(spaceId = "abc")
                }
                verify(exactly = 0) { spaceVisitRepository.save(any()) }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            spaceId: String = "10",
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                spaceId = spaceId,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}

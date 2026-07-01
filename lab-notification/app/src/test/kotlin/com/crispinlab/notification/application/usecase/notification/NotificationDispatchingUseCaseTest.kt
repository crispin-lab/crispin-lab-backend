package com.crispinlab.notification.application.usecase.notification

import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.notification.application.port.incoming.notification.NotificationDispatching.Request
import com.crispinlab.notification.application.port.outgoing.notification.NotificationRepository
import com.crispinlab.notification.domain.notification.Notification
import com.crispinlab.notification.domain.notification.NotificationType
import com.crispinlab.notification.domain.notification.SourceType
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class NotificationDispatchingUseCaseTest :
    DescribeSpec({
        val notificationRepository = mockk<NotificationRepository>()
        val idGenerator = mockk<IdGenerator>()
        val useCase =
            NotificationDispatchingUseCase(
                notificationRepository = notificationRepository,
                idGenerator = idGenerator,
                transactionProvider = DummyTransactionProvider()
            )

        beforeEach {
            clearMocks(notificationRepository, idGenerator)
            every {
                notificationRepository.existingUserIdsAmong(any(), any(), any(), any())
            } returns emptySet()
            every { notificationRepository.saveAll(any()) } answers { firstArg() }
            every { idGenerator.next() } returnsMany (1L..1000L).toList()
        }

        describe("알림 발사") {
            it("targetUserIds 가 비어 있으면 아무 것도 하지 않는다") {
                useCase.perform(basicRequest(targetUserIds = emptyList()))

                verify(exactly = 0) { notificationRepository.saveAll(any()) }
            }

            it("모든 target 이 신규면 saveAll 에 모두 들어간다") {
                val saved = slot<List<Notification>>()
                every { notificationRepository.saveAll(capture(saved)) } answers { saved.captured }

                useCase.perform(
                    basicRequest(targetUserIds = listOf(UserId(200L), UserId(201L)))
                )

                saved.captured.map { it.userId } shouldBe listOf(UserId(200L), UserId(201L))
                saved.captured.all { it.type == NotificationType.MENTION } shouldBe true
                saved.captured.all { it.sourceType == SourceType.PAGE } shouldBe true
            }

            it("existingUserIdsAmong 에 포함된 target 은 발사 대상에서 제외된다 (재발사 차단)") {
                every {
                    notificationRepository.existingUserIdsAmong(any(), any(), any(), any())
                } returns setOf(UserId(200L))
                val saved = slot<List<Notification>>()
                every { notificationRepository.saveAll(capture(saved)) } answers { saved.captured }

                useCase.perform(
                    basicRequest(targetUserIds = listOf(UserId(200L), UserId(201L)))
                )

                saved.captured.map { it.userId } shouldBe listOf(UserId(201L))
            }

            it("모든 target 이 이미 알림 받았으면 saveAll 은 빈 리스트로 호출된다") {
                every {
                    notificationRepository.existingUserIdsAmong(any(), any(), any(), any())
                } returns setOf(UserId(200L), UserId(201L))
                val saved = slot<List<Notification>>()
                every { notificationRepository.saveAll(capture(saved)) } answers { saved.captured }

                useCase.perform(
                    basicRequest(targetUserIds = listOf(UserId(200L), UserId(201L)))
                )

                saved.captured shouldBe emptyList()
            }

            it("batch lookup — existingUserIdsAmong 은 targetUserIds 당 1회만 호출된다 (N+1 방지)") {
                useCase.perform(
                    basicRequest(
                        targetUserIds = listOf(UserId(200L), UserId(201L), UserId(202L))
                    )
                )

                verify(exactly = 1) {
                    notificationRepository.existingUserIdsAmong(any(), any(), any(), any())
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            sourceType: SourceType = SourceType.PAGE,
            sourceId: Long = 10L,
            type: NotificationType = NotificationType.MENTION,
            targetUserIds: List<UserId> = listOf(UserId(200L)),
            actorUserId: UserId = UserId(100L)
        ): Request =
            Request(
                sourceType = sourceType,
                sourceId = sourceId,
                type = type,
                targetUserIds = targetUserIds,
                actorUserId = actorUserId
            )
    }
}

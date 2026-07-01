package com.crispinlab.notification.application.usecase.notification

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.DummyTransactionProvider
import com.crispinlab.notification.application.port.incoming.notification.NotificationReading.Request
import com.crispinlab.notification.application.port.outgoing.notification.NotificationRepository
import com.crispinlab.notification.domain.access.Viewer
import com.crispinlab.notification.domain.notification.Notification
import com.crispinlab.notification.domain.notification.NotificationId
import com.crispinlab.notification.domain.notification.NotificationType
import com.crispinlab.notification.domain.notification.SourceType
import com.crispinlab.user.domain.user.UserId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant

class NotificationReadingUseCaseTest :
    DescribeSpec({
        val notificationRepository = mockk<NotificationRepository>()
        val useCase =
            NotificationReadingUseCase(
                notificationRepository = notificationRepository,
                transactionProvider = DummyTransactionProvider()
            )
        val occurredAt: Instant = Instant.parse("2026-01-01T00:00:00Z")

        fun basicNotification(
            id: Long = 1L,
            userId: Long = 100L,
            isRead: Boolean = false
        ): Notification =
            Notification(
                id = NotificationId(id),
                userId = UserId(userId),
                type = NotificationType.MENTION,
                sourceType = SourceType.PAGE,
                sourceId = 10L,
                actorUserId = UserId(999L),
                createdAt = occurredAt,
                isRead = isRead
            )

        beforeEach {
            clearMocks(notificationRepository)
            every { notificationRepository.save(any()) } answers { firstArg() }
        }

        describe("알림 읽음 처리") {
            it("본인 알림이면 markAsRead 후 save 한다") {
                every {
                    notificationRepository.findBy(NotificationId(1L))
                } returns basicNotification(id = 1L, userId = 100L)
                val saved = slot<Notification>()
                every { notificationRepository.save(capture(saved)) } answers { saved.captured }

                useCase.perform(basicRequest(notificationId = "1", userId = UserId(100L)))

                saved.captured.isRead shouldBe true
                saved.captured.readAt.shouldNotBeNull()
            }

            it("이미 읽음 처리된 알림에 read 호출은 멱등 (noop)") {
                every {
                    notificationRepository.findBy(NotificationId(1L))
                } returns basicNotification(id = 1L, userId = 100L, isRead = true)
                val saved = slot<Notification>()
                every { notificationRepository.save(capture(saved)) } answers { saved.captured }

                useCase.perform(basicRequest(notificationId = "1", userId = UserId(100L)))

                saved.captured.isRead shouldBe true
            }

            it("알림이 없으면 NotFoundException") {
                every { notificationRepository.findBy(any()) } returns null

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest())
                }
                verify(exactly = 0) { notificationRepository.save(any()) }
            }

            it("다른 사용자의 알림 읽음 처리는 NotFoundException (IDOR)") {
                every {
                    notificationRepository.findBy(NotificationId(1L))
                } returns basicNotification(id = 1L, userId = 999L)

                shouldThrow<NotFoundException> {
                    useCase.perform(basicRequest(userId = UserId(100L)))
                }
                verify(exactly = 0) { notificationRepository.save(any()) }
            }

            it("notificationId 형식이 잘못되면 Request 생성에서 실패") {
                shouldThrow<IllegalArgumentException> {
                    basicRequest(notificationId = "not-a-number")
                }
            }
        }
    }) {
    companion object {
        fun basicRequest(
            notificationId: String = "1",
            userId: UserId = UserId(100L),
            isAdmin: Boolean = false
        ): Request =
            Request(
                notificationId = notificationId,
                viewer = Viewer.Member(userId = userId, isAdmin = isAdmin)
            )
    }
}

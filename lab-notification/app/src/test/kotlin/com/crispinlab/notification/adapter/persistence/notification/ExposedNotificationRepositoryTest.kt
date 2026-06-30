package com.crispinlab.notification.adapter.persistence.notification

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.persistence.PostgresTestContext
import com.crispinlab.notification.domain.notification.Notification
import com.crispinlab.notification.domain.notification.NotificationId
import com.crispinlab.notification.domain.notification.NotificationType
import com.crispinlab.notification.domain.notification.SourceType
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ExposedNotificationRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedNotificationRepository()
        val occurredAt: Instant = Instant.parse("2026-01-01T00:00:00Z")

        afterEach { PostgresTestContext.truncateAll() }

        fun mentionFor(
            id: Long,
            userId: Long,
            sourceId: Long = 10L,
            actorUserId: Long = 999L,
            isRead: Boolean = false,
            readAt: Instant? = null
        ): Notification =
            Notification(
                id = NotificationId(id),
                userId = UserId(userId),
                type = NotificationType.MENTION,
                sourceType = SourceType.PAGE,
                sourceId = sourceId,
                actorUserId = UserId(actorUserId),
                isRead = isRead,
                readAt = readAt,
                createdAt = occurredAt
            )

        describe("ExposedNotificationRepository") {
            it("save 후 findBy 로 동일 entity 가 복원된다") {
                transaction(database) {
                    repository.save(mentionFor(id = 1L, userId = 100L))
                }

                transaction(database) {
                    val found = repository.findBy(NotificationId(1L))
                    found.shouldNotBeNull()
                    found.id shouldBe NotificationId(1L)
                    found.userId shouldBe UserId(100L)
                    found.type shouldBe NotificationType.MENTION
                    found.sourceType shouldBe SourceType.PAGE
                    found.sourceId shouldBe 10L
                    found.actorUserId shouldBe UserId(999L)
                    found.isRead shouldBe false
                    found.readAt.shouldBeNull()
                }
            }

            it("markAsRead 후 save 는 isRead 와 readAt 만 갱신한다") {
                transaction(database) {
                    repository.save(mentionFor(id = 2L, userId = 100L))
                }
                transaction(database) {
                    val notification = repository.findBy(NotificationId(2L)).shouldNotBeNull()
                    notification.markAsRead()
                    repository.save(notification)
                }
                transaction(database) {
                    val updated = repository.findBy(NotificationId(2L)).shouldNotBeNull()
                    updated.isRead shouldBe true
                    updated.readAt.shouldNotBeNull()
                    updated.createdAt shouldBe occurredAt
                }
            }

            it("saveAll 의 ignore 로 unique 위반 row 는 skip 되고 신규만 들어간다") {
                transaction(database) {
                    repository.save(mentionFor(id = 1L, userId = 100L))
                }
                transaction(database) {
                    repository.saveAll(
                        listOf(
                            mentionFor(id = 2L, userId = 100L),
                            mentionFor(id = 3L, userId = 200L)
                        )
                    )
                }
                transaction(database) {
                    repository.findBy(NotificationId(2L)).shouldBeNull()
                    repository.findBy(NotificationId(3L)).shouldNotBeNull()
                }
            }

            it("search 는 본인 user 의 알림만 createdAt DESC 순으로 반환한다") {
                transaction(database) {
                    repository.saveAll(
                        listOf(
                            mentionFor(id = 1L, userId = 100L, sourceId = 11L),
                            mentionFor(id = 2L, userId = 100L, sourceId = 12L),
                            mentionFor(id = 3L, userId = 200L, sourceId = 13L)
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.search(
                            userId = UserId(100L),
                            unreadOnly = false,
                            pageRequest = PageRequest(page = 0, size = 20)
                        )
                    result.items shouldHaveSize 2
                    result.items.map { it.userId } shouldBe
                        listOf(UserId(100L), UserId(100L))
                    result.items.map { it.id } shouldBe
                        listOf(NotificationId(2L), NotificationId(1L))
                }
            }

            it("search unreadOnly=true 는 미독 알림만 반환한다") {
                transaction(database) {
                    repository.saveAll(
                        listOf(
                            mentionFor(id = 1L, userId = 100L, sourceId = 11L, isRead = false),
                            mentionFor(
                                id = 2L,
                                userId = 100L,
                                sourceId = 12L,
                                isRead = true,
                                readAt = occurredAt
                            )
                        )
                    )
                }

                transaction(database) {
                    val result =
                        repository.search(
                            userId = UserId(100L),
                            unreadOnly = true,
                            pageRequest = PageRequest(page = 0, size = 20)
                        )
                    result.items shouldHaveSize 1
                    result.items.first().isRead shouldBe false
                }
            }

            it("existsBy 는 같은 (userId, type, sourceType, sourceId) 에 대해 true 를 반환한다") {
                transaction(database) {
                    repository.save(mentionFor(id = 1L, userId = 100L, sourceId = 10L))
                }

                transaction(database) {
                    repository.existsBy(
                        userId = UserId(100L),
                        type = NotificationType.MENTION,
                        sourceType = SourceType.PAGE,
                        sourceId = 10L
                    ) shouldBe true
                    repository.existsBy(
                        userId = UserId(100L),
                        type = NotificationType.MENTION,
                        sourceType = SourceType.PAGE,
                        sourceId = 999L
                    ) shouldBe false
                    repository.existsBy(
                        userId = UserId(999L),
                        type = NotificationType.MENTION,
                        sourceType = SourceType.PAGE,
                        sourceId = 10L
                    ) shouldBe false
                }
            }
        }
    })

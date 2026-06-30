package com.crispinlab.space.application.usecase.mention

import com.crispinlab.common.id.IdGenerator
import com.crispinlab.notification.application.port.incoming.notification.NotificationDispatching
import com.crispinlab.notification.domain.notification.NotificationType
import com.crispinlab.space.application.port.outgoing.mention.MentionRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.mention.Mention
import com.crispinlab.space.domain.mention.MentionId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.crispinlab.user.application.port.outgoing.user.UserAdminQuery
import com.crispinlab.user.domain.user.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import com.crispinlab.notification.domain.notification.SourceType as NotificationSourceType

class MentionDispatcherTest :
    DescribeSpec({
        val mentionRepository = mockk<MentionRepository>()
        val spaceMemberRepository = mockk<SpaceMemberRepository>()
        val userAdminQuery = mockk<UserAdminQuery>()
        val notificationDispatching = mockk<NotificationDispatching>()
        val idGenerator = mockk<IdGenerator>()
        val dispatcher =
            MentionDispatcher(
                mentionRepository = mentionRepository,
                spaceMemberRepository = spaceMemberRepository,
                userAdminQuery = userAdminQuery,
                notificationDispatching = notificationDispatching,
                idGenerator = idGenerator
            )

        beforeEach {
            clearMocks(
                mentionRepository,
                spaceMemberRepository,
                userAdminQuery,
                notificationDispatching,
                idGenerator
            )
            every { idGenerator.next() } returnsMany (1L..1000L).toList()
            every { userAdminQuery.adminsAmong(any()) } returns emptySet()
            every { spaceMemberRepository.findSpaceIdsByUserIds(any()) } returns emptyMap()
            every { notificationDispatching.perform(any()) } returns Unit
        }

        fun dispatch(
            extracted: List<UserId>,
            actorUserId: UserId = UserId(100L),
            pageVisibility: Visibility = Visibility.PUBLIC,
            spaceVisibility: SpaceVisibility = SpaceVisibility.PUBLIC,
            pageAuthorId: UserId = UserId(100L)
        ) {
            dispatcher.dispatch(
                sourceType = Mention.SourceType.PAGE,
                sourceId = 10L,
                actorUserId = actorUserId,
                extracted = extracted,
                subject =
                    MentionDispatcher.MentionSubject(
                        spaceId = SpaceId(20L),
                        spaceVisibility = spaceVisibility,
                        pageVisibility = pageVisibility,
                        pageAuthorId = pageAuthorId
                    ),
                occurredAt = DUMMY_INSTANT
            )
        }

        describe("dispatch") {
            it("extracted 가 비면 replaceMentionsFor 만 빈 list 로 호출 + notification 미발사") {
                every {
                    mentionRepository.replaceMentionsFor(any(), any(), any())
                } returns emptyList()

                dispatch(extracted = emptyList())

                verify(exactly = 1) {
                    mentionRepository.replaceMentionsFor(
                        Mention.SourceType.PAGE,
                        10L,
                        emptyList()
                    )
                }
                verify(exactly = 0) { notificationDispatching.perform(any()) }
            }

            it("자기 자신 멘션은 candidate 에서 제외 — 모두 self 면 clear 와 동일") {
                every {
                    mentionRepository.replaceMentionsFor(any(), any(), any())
                } returns emptyList()

                dispatch(
                    extracted = listOf(UserId(100L)),
                    actorUserId = UserId(100L)
                )

                verify(exactly = 1) {
                    mentionRepository.replaceMentionsFor(
                        Mention.SourceType.PAGE,
                        10L,
                        emptyList()
                    )
                }
                verify(exactly = 0) { notificationDispatching.perform(any()) }
            }

            it("PUBLIC 페이지 — 모든 candidate 가 permitted, newlyAdded 만 dispatch 호출") {
                val replaced = slot<List<Mention>>()
                every {
                    mentionRepository.replaceMentionsFor(any(), any(), capture(replaced))
                } answers { listOf(replaced.captured.first()) }

                dispatch(extracted = listOf(UserId(200L), UserId(201L)))

                replaced.captured.map { it.mentionedUserId } shouldContainExactlyInAnyOrder
                    listOf(UserId(200L), UserId(201L))
                verify(exactly = 1) {
                    notificationDispatching.perform(
                        match {
                            it.sourceType == NotificationSourceType.PAGE &&
                                it.sourceId == 10L &&
                                it.type == NotificationType.MENTION &&
                                it.actorUserId == UserId(100L) &&
                                it.targetUserIds == listOf(UserId(200L))
                        }
                    )
                }
            }

            it("DRAFT 페이지에서 일반 user 는 permitted 에서 제외") {
                val replaced = slot<List<Mention>>()
                every {
                    mentionRepository.replaceMentionsFor(any(), any(), capture(replaced))
                } answers { replaced.captured }

                dispatch(
                    extracted = listOf(UserId(200L)),
                    pageVisibility = Visibility.DRAFT
                )

                replaced.captured.shouldBeEmpty()
                verify(exactly = 0) { notificationDispatching.perform(any()) }
            }

            it("ADMIN 사용자는 DRAFT 페이지에서도 멘션 대상 (Privileged 우회)") {
                every { userAdminQuery.adminsAmong(any()) } returns setOf(UserId(200L))
                val replaced = slot<List<Mention>>()
                every {
                    mentionRepository.replaceMentionsFor(any(), any(), capture(replaced))
                } answers { replaced.captured }

                dispatch(
                    extracted = listOf(UserId(200L), UserId(201L)),
                    pageVisibility = Visibility.DRAFT
                )

                replaced.captured.map { it.mentionedUserId } shouldBe listOf(UserId(200L))
                verify(exactly = 1) { notificationDispatching.perform(any()) }
            }

            it("MEMBER 페이지 — 같은 space 멤버만 permitted") {
                every {
                    spaceMemberRepository.findSpaceIdsByUserIds(any())
                } returns mapOf(UserId(200L) to setOf(SpaceId(20L)))
                val replaced = slot<List<Mention>>()
                every {
                    mentionRepository.replaceMentionsFor(any(), any(), capture(replaced))
                } answers { replaced.captured }

                dispatch(
                    extracted = listOf(UserId(200L), UserId(201L)),
                    pageVisibility = Visibility.MEMBER
                )

                replaced.captured.map { it.mentionedUserId } shouldBe listOf(UserId(200L))
            }

            it("newlyAdded 가 비면 notification 미발사 (idempotent 재발사 차단)") {
                every {
                    mentionRepository.replaceMentionsFor(any(), any(), any())
                } returns emptyList()

                dispatch(extracted = listOf(UserId(200L), UserId(201L)))

                verify(exactly = 0) { notificationDispatching.perform(any()) }
            }

            it("batch lookup — adminsAmong 와 findSpaceIdsByUserIds 는 각 1회만 호출 (N+1 방지)") {
                every {
                    mentionRepository.replaceMentionsFor(any(), any(), any())
                } returns emptyList()

                dispatch(extracted = listOf(UserId(200L), UserId(201L), UserId(202L)))

                verify(exactly = 1) { userAdminQuery.adminsAmong(any()) }
                verify(exactly = 1) { spaceMemberRepository.findSpaceIdsByUserIds(any()) }
            }
        }
    })

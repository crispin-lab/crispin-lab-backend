package com.crispinlab.space.application.usecase.mention

import com.crispinlab.common.id.IdGenerator
import com.crispinlab.notification.application.port.incoming.notification.NotificationDispatching
import com.crispinlab.notification.domain.notification.NotificationType
import com.crispinlab.space.application.port.outgoing.mention.MentionRepository
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.mention.Mention
import com.crispinlab.space.domain.mention.MentionId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.user.application.port.outgoing.user.UserAdminQuery
import com.crispinlab.user.domain.user.UserId
import java.time.Instant
import org.springframework.stereotype.Component
import com.crispinlab.notification.domain.notification.SourceType as NotificationSourceType

@Component
class MentionDispatcher(
    private val mentionRepository: MentionRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val userAdminQuery: UserAdminQuery,
    private val notificationDispatching: NotificationDispatching,
    private val idGenerator: IdGenerator
) {
    fun dispatch(
        sourceType: Mention.SourceType,
        sourceId: Long,
        actorUserId: UserId,
        extracted: List<UserId>,
        subject: MentionSubject,
        occurredAt: Instant
    ) {
        val candidates: List<UserId> = extracted.filter { it != actorUserId }
        if (candidates.isEmpty()) {
            mentionRepository.replaceMentionsFor(sourceType, sourceId, emptyList())
            return
        }
        val permitted: List<UserId> = candidates.filterMentionableFor(subject)
        val mentions: List<Mention> =
            permitted.map { userId ->
                Mention(
                    id = MentionId(idGenerator.next()),
                    sourceType = sourceType,
                    sourceId = sourceId,
                    mentionedUserId = userId,
                    mentionedByUserId = actorUserId,
                    createdAt = occurredAt
                )
            }
        val newlyAdded =
            mentionRepository.replaceMentionsFor(sourceType, sourceId, mentions)
        if (newlyAdded.isEmpty()) return
        notificationDispatching.perform(
            NotificationDispatching.Request(
                sourceType =
                    when (sourceType) {
                        Mention.SourceType.PAGE -> NotificationSourceType.PAGE
                        Mention.SourceType.COMMENT -> NotificationSourceType.COMMENT
                    },
                sourceId = sourceId,
                type = NotificationType.MENTION,
                targetUserIds = newlyAdded.map { it.mentionedUserId },
                actorUserId = actorUserId
            )
        )
    }

    private fun List<UserId>.filterMentionableFor(subject: MentionSubject): List<UserId> {
        val adminUserIds: Set<UserId> = userAdminQuery.adminsAmong(this)
        val membershipByUser: Map<UserId, Set<SpaceId>> =
            spaceMemberRepository.findSpaceIdsByUserIds(this)
        return filter { userId ->
            val viewer = Viewer.Member(userId = userId, isAdmin = userId in adminUserIds)
            VisibilityScope
                .of(viewer = viewer, memberOfSpaceIds = membershipByUser[userId].orEmpty())
                .allows(
                    pageVisibility = subject.pageVisibility,
                    spaceVisibility = subject.spaceVisibility,
                    spaceId = subject.spaceId,
                    authorId = subject.pageAuthorId
                )
        }
    }

    data class MentionSubject(
        val spaceId: SpaceId,
        val spaceVisibility: SpaceVisibility,
        val pageVisibility: Visibility,
        val pageAuthorId: UserId
    )
}

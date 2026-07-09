package com.crispinlab.composition.application.usecase.mention

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.mention.MentionSuggesting
import com.crispinlab.composition.application.port.incoming.mention.MentionSuggesting.Companion.CANDIDATE_MULTIPLIER
import com.crispinlab.composition.application.port.incoming.mention.MentionSuggesting.Request
import com.crispinlab.composition.application.port.incoming.mention.MentionSuggesting.Result
import com.crispinlab.composition.application.port.outgoing.space.SpaceMembershipLookup
import com.crispinlab.composition.application.port.outgoing.user.UserAdminLookup
import com.crispinlab.space.application.port.outgoing.page.PageSearchPort.VisibilityScope
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceErrorCode
import com.crispinlab.user.application.port.incoming.user.UserSearching
import org.springframework.stereotype.Service

@Service
class MentionSuggestingUseCase(
    private val userSearching: UserSearching,
    private val spaceMembershipLookup: SpaceMembershipLookup,
    private val userAdminLookup: UserAdminLookup,
    private val transactionProvider: TransactionProvider
) : MentionSuggesting {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .also { it.validate() }
                .toDomainRequest()
                .let { userSearching.perform(it) }
                .filterFor(request)
                .take(request.size)
                .toResult()
        }

    private fun Request.validate() {
        if (viewer.isAdmin) return
        val memberSpaceIds = spaceMembershipLookup.memberSpaceIdsOf(viewer)
        if (spaceId !in memberSpaceIds) {
            throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)
        }
    }

    private fun Request.toDomainRequest(): UserSearching.Request =
        UserSearching.Request(
            query = query,
            size = minOf(size * CANDIDATE_MULTIPLIER, UserSearching.Request.MAX_SIZE)
        )

    private fun UserSearching.Result.filterFor(request: Request): List<UserSearching.Result.Item> {
        if (items.isEmpty()) return emptyList()
        val userIds = items.map { it.userId }.toSet()
        val adminIds = userAdminLookup.adminsAmong(userIds)
        val memberships = spaceMembershipLookup.membershipsOf(userIds, request.viewer)
        return items.filter { item ->
            val candidateViewer =
                Viewer.Member(
                    userId = item.userId,
                    isAdmin = item.userId in adminIds
                )
            VisibilityScope
                .of(
                    viewer = candidateViewer,
                    memberOfSpaceIds = memberships[item.userId].orEmpty()
                ).allows(
                    pageVisibility = request.pageVisibility,
                    spaceVisibility = request.spaceVisibility,
                    spaceId = request.spaceId,
                    authorId = request.pageAuthorId
                )
        }
    }

    private fun List<UserSearching.Result.Item>.toResult(): Result =
        Result(
            items =
                map {
                    Result.Item(
                        userId = it.userId,
                        handle = it.handle
                    )
                }
        )
}

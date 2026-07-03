package com.crispinlab.composition.application.usecase.user

import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.user.UserSearchingComposition
import com.crispinlab.composition.application.port.incoming.user.UserSearchingComposition.Request
import com.crispinlab.composition.application.port.incoming.user.UserSearchingComposition.Result
import com.crispinlab.composition.application.port.outgoing.space.SpaceMembershipLookup
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.application.port.incoming.user.UserSearching
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Service

@Service
class UserSearchingCompositionUseCase(
    private val userSearching: UserSearching,
    private val spaceMembershipLookup: SpaceMembershipLookup,
    private val transactionProvider: TransactionProvider
) : UserSearchingComposition {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .toDomainRequest()
                .let { userSearching.perform(it) }
                .toResultFor(request.viewer)
        }

    private fun Request.toDomainRequest(): UserSearching.Request =
        UserSearching.Request(
            query = query,
            size = size
        )

    private fun UserSearching.Result.toResultFor(viewer: Viewer.Member): Result {
        val memberships =
            runCatching {
                spaceMembershipLookup.membershipsOf(
                    userIds = items.map { it.userId }.toSet(),
                    viewer = viewer
                )
            }.getOrElse { emptyMap() }
        return Result(items = items.map { it.toItem(memberships) })
    }

    private fun UserSearching.Result.Item.toItem(
        memberships: Map<UserId, Set<SpaceId>>
    ): Result.Item =
        Result.Item(
            userId = userId,
            handle = handle,
            memberOfSpaceIds =
                memberships[userId]
                    .orEmpty()
                    .sortedBy { it.value }
        )
}

package com.crispinlab.composition.application.usecase.spacemember

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.spacemember.SpaceMemberListingComposition
import com.crispinlab.composition.application.port.incoming.spacemember.SpaceMemberListingComposition.Request
import com.crispinlab.composition.application.port.incoming.spacemember.SpaceMemberListingComposition.Result
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberListing
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberListing.Summary
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Service

@Service
class SpaceMemberListingCompositionUseCase(
    private val spaceMemberListing: SpaceMemberListing,
    private val userHandleLookup: UserHandleLookup,
    private val transactionProvider: TransactionProvider
) : SpaceMemberListingComposition {
    override fun perform(request: Request): PageResult<Result> =
        transactionProvider.transactional(readOnly = true) {
            request
                .toDomainRequest()
                .let { spaceMemberListing.perform(it) }
                .toResults()
        }

    private fun Request.toDomainRequest(): SpaceMemberListing.Request =
        SpaceMemberListing.Request(
            spaceId = spaceId,
            page = page,
            size = size,
            viewer = viewer
        )

    private fun PageResult<Summary>.toResults(): PageResult<Result> {
        val handles = userHandleLookup.handlesOf(items.map { it.userId }.toSet())
        return map { it.toResult(handles) }
    }

    private fun Summary.toResult(handles: Map<UserId, String>): Result =
        Result(
            spaceMemberId = spaceMemberId,
            spaceId = spaceId,
            userId = userId,
            role = role,
            joinedAt = joinedAt,
            handle = handles[userId] ?: ""
        )
}

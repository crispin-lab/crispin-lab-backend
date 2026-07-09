package com.crispinlab.composition.application.usecase.audit

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.composition.application.port.incoming.audit.SpaceAuditEntryListingComposition
import com.crispinlab.composition.application.port.incoming.audit.SpaceAuditEntryListingComposition.Request
import com.crispinlab.composition.application.port.incoming.audit.SpaceAuditEntryListingComposition.Result
import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.space.application.port.incoming.audit.SpaceAuditEntryListing
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Service

@Service
class SpaceAuditEntryListingCompositionUseCase(
    private val spaceAuditEntryListing: SpaceAuditEntryListing,
    private val userHandleLookup: UserHandleLookup,
    private val transactionProvider: TransactionProvider
) : SpaceAuditEntryListingComposition {
    override fun perform(request: Request): PageResult<Result> =
        transactionProvider.transactional(readOnly = true) {
            request
                .toDomainRequest()
                .let { spaceAuditEntryListing.perform(it) }
                .toResults()
        }

    private fun Request.toDomainRequest(): SpaceAuditEntryListing.Request =
        SpaceAuditEntryListing.Request(
            spaceId = spaceId,
            page = page,
            size = size,
            viewer = viewer
        )

    private fun PageResult<SpaceAuditEntryListing.Result>.toResults(): PageResult<Result> {
        val handles =
            runCatching {
                userHandleLookup.handlesOf(items.map { it.actorUserId }.toSet())
            }.getOrElse { emptyMap() }
        return map { it.toResult(handles) }
    }

    private fun SpaceAuditEntryListing.Result.toResult(handles: Map<UserId, String>): Result =
        Result(
            id = id,
            actorUserId = actorUserId,
            actorHandle = handles[actorUserId] ?: "",
            action = action,
            changeSummary = changeSummary,
            createdAt = createdAt
        )
}

package com.crispinlab.space.application.usecase.audit

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.audit.SpaceAuditEntryListing
import com.crispinlab.space.application.port.incoming.audit.SpaceAuditEntryListing.Request
import com.crispinlab.space.application.port.incoming.audit.SpaceAuditEntryListing.Result
import com.crispinlab.space.application.port.outgoing.audit.SpaceAuditRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.requireSpaceEditPermission
import com.crispinlab.space.domain.audit.SpaceAuditEntry
import com.crispinlab.space.domain.space.SpaceErrorCode
import org.springframework.stereotype.Service

@Service
class SpaceAuditEntryListingUseCase(
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val spaceAuditRepository: SpaceAuditRepository,
    private val transactionProvider: TransactionProvider
) : SpaceAuditEntryListing {
    override fun perform(request: Request): PageResult<Result> =
        transactionProvider.transactional(readOnly = true) {
            request
                .also { it.validate() }
                .let {
                    spaceAuditRepository.findBySpaceId(
                        spaceId = it.spaceId,
                        pageRequest = it.pageRequest
                    )
                }.map { it.toResult() }
        }

    private fun Request.validate() {
        if (viewer.isAdmin) return
        spaceMemberRepository.requireSpaceEditPermission(
            viewer = viewer,
            spaceId = spaceId
        )
        spaceRepository.findBy(spaceId)
            ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)
    }

    private fun SpaceAuditEntry.toResult(): Result =
        Result(
            id = id,
            actorUserId = actorUserId,
            action = action,
            changeSummary = changeSummary.json,
            createdAt = createdAt
        )
}

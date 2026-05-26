package com.crispinlab.space.application.usecase.spacemember

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberListing
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberListing.Request
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberListing.Summary
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceVisibilityScope
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.memberSpaceIdsOf
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceErrorCode
import com.crispinlab.space.domain.spacemember.SpaceMember
import org.springframework.stereotype.Service

@Service
class SpaceMemberListingUseCase(
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : SpaceMemberListing {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request
                .also { it.validate() }
                .toResult()
        }

    private fun Request.validate() {
        val space: Space =
            spaceRepository.findBy(spaceId)
                ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)
        val scope =
            SpaceVisibilityScope.of(viewer, spaceMemberRepository.memberSpaceIdsOf(viewer))
        if (!scope.allows(space.visibility, space.id)) {
            throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)
        }
    }

    private fun Request.toResult(): PageResult<Summary> =
        spaceMemberRepository
            .findBySpaceId(spaceId, pageRequest)
            .map { it.toSummary() }

    private fun SpaceMember.toSummary(): Summary =
        Summary(
            spaceMemberId = id,
            spaceId = spaceId,
            userId = userId,
            role = role,
            joinedAt = joinedAt
        )
}

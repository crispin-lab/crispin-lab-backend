package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceListing
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Request
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Summary
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceVisibilityScope
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.memberSpaceIdsOf
import org.springframework.stereotype.Service

@Service
class SpaceListingUseCase(
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : SpaceListing {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request.toResult()
        }

    private fun Request.toResult(): PageResult<Summary> =
        spaceRepository
            .findPage(
                pageRequest = pageRequest,
                scope =
                    SpaceVisibilityScope.of(
                        viewer,
                        spaceMemberRepository.memberSpaceIdsOf(viewer)
                    ),
                keyword = keyword,
                sort = sort,
                direction = direction
            ).map { it.toSummary() }

    private fun SpaceRepository.Summary.toSummary(): Summary =
        Summary(
            spaceId = spaceId,
            name = name,
            description = description,
            visibility = visibility,
            lastActivityAt = lastActivityAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

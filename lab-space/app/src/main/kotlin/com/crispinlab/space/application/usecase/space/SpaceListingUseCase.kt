package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceListing
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Request
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Summary
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceVisibility
import org.springframework.stereotype.Service

@Service
class SpaceListingUseCase(
    private val spaceRepository: SpaceRepository,
    private val transactionProvider: TransactionProvider
) : SpaceListing {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request.toResult()
        }

    private fun Request.toResult(): PageResult<Summary> =
        spaceRepository
            .findPage(pageRequest, viewer.allowedSpaceVisibilities())
            .map { it.toSummary() }

    private fun Viewer.allowedSpaceVisibilities(): Set<SpaceVisibility> =
        when (this) {
            is Viewer.Anonymous -> setOf(SpaceVisibility.PUBLIC)
            is Viewer.Member -> setOf(SpaceVisibility.PUBLIC, SpaceVisibility.INTERNAL)
        }

    private fun Space.toSummary(): Summary =
        Summary(
            spaceId = id,
            name = name,
            description = description,
            visibility = visibility,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

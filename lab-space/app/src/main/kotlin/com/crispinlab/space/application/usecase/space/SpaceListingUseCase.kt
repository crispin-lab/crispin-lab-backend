package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceListing
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Request
import com.crispinlab.space.application.port.incoming.space.SpaceListing.Summary
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.Space
import org.springframework.stereotype.Service

@Service
class SpaceListingUseCase(
    private val spaceRepository: SpaceRepository,
    private val transactionProvider: TransactionProvider
) : SpaceListing {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request
                .also { it.validate() }
                .toResult()
        }

    private fun Request.validate() {
        /*
        todo    :: 외부 의존성이 필요한 검증을 둘 자리. 권한 등이 도입될 때 채운다.
         author :: heechoel shin
         date   :: 2026-05-13T11:00:00KST
         ticket :: LAB-29
         */
    }

    private fun Request.toResult(): PageResult<Summary> =
        spaceRepository
            .findPage(pageRequest)
            .map { it.toSummary() }

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

package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.tag.TagListing
import com.crispinlab.space.application.port.incoming.tag.TagListing.Request
import com.crispinlab.space.application.port.incoming.tag.TagListing.Summary
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.space.SpaceErrorCode
import com.crispinlab.space.domain.tag.Tag
import org.springframework.stereotype.Service

@Service
class TagListingUseCase(
    private val tagRepository: TagRepository,
    private val spaceRepository: SpaceRepository,
    private val transactionProvider: TransactionProvider
) : TagListing {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request
                .also {
                    it.validate()
                }.toResult()
        }

    private fun Request.validate() {
        spaceRepository.findBy(spaceId)
            ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)
    }

    private fun Request.toResult(): PageResult<Summary> =
        tagRepository
            .findBySpaceId(spaceId, pageRequest)
            .map { it.toSummary() }

    private fun Tag.toSummary(): Summary =
        Summary(
            tagId = id,
            spaceId = spaceId,
            name = name,
            createdAt = createdAt
        )
}

package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.tag.PageTagListing
import com.crispinlab.space.application.port.incoming.tag.PageTagListing.Request
import com.crispinlab.space.application.port.incoming.tag.PageTagListing.Summary
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.tag.Tag
import org.springframework.stereotype.Service

@Service
class PageTagListingUseCase(
    private val tagRepository: TagRepository,
    private val pageRepository: PageRepository,
    private val transactionProvider: TransactionProvider
) : PageTagListing {
    override fun perform(request: Request): PageResult<Summary> =
        transactionProvider.transactional(readOnly = true) {
            request
                .also {
                    it.validate()
                }.toResult()
        }

    private fun Request.validate() {
        pageRepository.findBy(pageId)
            ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
    }

    private fun Request.toResult(): PageResult<Summary> =
        tagRepository
            .findTagsByPageId(pageId, pageRequest)
            .map { it.toSummary() }

    private fun Tag.toSummary(): Summary =
        Summary(
            tagId = id,
            spaceId = spaceId,
            name = name,
            createdAt = createdAt
        )
}

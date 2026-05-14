package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.tag.PageTagAttaching
import com.crispinlab.space.application.port.incoming.tag.PageTagAttaching.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.page.PageErrorCode
import com.crispinlab.space.domain.tag.PageTag
import com.crispinlab.space.domain.tag.TagErrorCode
import java.time.Instant.now
import org.springframework.stereotype.Service

@Service
class PageTagAttachingUseCase(
    private val tagRepository: TagRepository,
    private val pageRepository: PageRepository,
    private val transactionProvider: TransactionProvider
) : PageTagAttaching {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            request
                .also {
                    it.validate()
                }.toPageTag()
                .let {
                    tagRepository.attach(it)
                }
        }
    }

    private fun Request.validate() {
        val page =
            pageRepository.findBy(pageId)
                ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
        tagRepository
            .findBy(tagId)
            ?.takeIf { it.spaceId == page.spaceId }
            ?: throw NotFoundException(TagErrorCode.TAG_NOT_FOUND)
    }

    private fun Request.toPageTag(): PageTag =
        PageTag(
            pageId = pageId,
            tagId = tagId,
            createdAt = now()
        )
}

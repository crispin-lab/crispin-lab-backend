package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.tag.PageTagDetaching
import com.crispinlab.space.application.port.incoming.tag.PageTagDetaching.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.page.PageErrorCode
import org.springframework.stereotype.Service

@Service
class PageTagDetachingUseCase(
    private val tagRepository: TagRepository,
    private val pageRepository: PageRepository,
    private val transactionProvider: TransactionProvider
) : PageTagDetaching {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            request
                .also {
                    it.validate()
                }.let {
                    tagRepository.detach(it.pageId, it.tagId)
                }
        }
    }

    private fun Request.validate() {
        pageRepository
            .findBy(pageId)
            ?.takeIf {
                viewer.isAdmin || it.authorId == viewer.userId
            } ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
    }
}

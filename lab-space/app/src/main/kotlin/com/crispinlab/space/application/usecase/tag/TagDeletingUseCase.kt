package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.tag.TagDeleting
import com.crispinlab.space.application.port.incoming.tag.TagDeleting.Request
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import com.crispinlab.space.domain.tag.TagErrorCode
import org.springframework.stereotype.Service

@Service
class TagDeletingUseCase(
    private val tagRepository: TagRepository,
    private val transactionProvider: TransactionProvider
) : TagDeleting {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            request
                .also {
                    it.validate()
                }.let {
                    tagRepository.delete(it.tagId)
                }
        }
    }

    private fun Request.validate() {
        if (!viewer.isAdmin) {
            throw ForbiddenException(TagErrorCode.TAG_ADMIN_ONLY)
        }
    }
}

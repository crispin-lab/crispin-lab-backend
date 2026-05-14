package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.tag.TagDeleting
import com.crispinlab.space.application.port.incoming.tag.TagDeleting.Request
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import org.springframework.stereotype.Service

@Service
class TagDeletingUseCase(
    private val tagRepository: TagRepository,
    private val transactionProvider: TransactionProvider
) : TagDeleting {
    // 멱등 — 이미 없거나 race 로 동시 삭제돼도 204. "tag 가 없도록 만들어 달라" 는 의도라 race 마다 404 분기가 무의미.
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
        /*
        todo    :: 권한 모델 도입 시 currentUserId 기반 소유자 검증 추가.
         author :: heechoel shin
         date   :: 2026-05-15T09:00:00KST
         ticket :: LAB-24
         */
    }
}

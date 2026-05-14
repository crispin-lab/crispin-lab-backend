package com.crispinlab.space.application.usecase.tag

import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.tag.PageTagDetaching
import com.crispinlab.space.application.port.incoming.tag.PageTagDetaching.Request
import com.crispinlab.space.application.port.outgoing.tag.TagRepository
import org.springframework.stereotype.Service

@Service
class PageTagDetachingUseCase(
    private val tagRepository: TagRepository,
    private val transactionProvider: TransactionProvider
) : PageTagDetaching {
    // detach 는 의도적으로 멱등 — 매핑이 없거나 cross-space 의 매핑이라 영향이 없어도 204.
    // 외부에서 noop 과 실제 detach 가 응답으로 구분되지 않아 IDOR/enumeration 위험이 없다.
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
        /*
        todo    :: 권한 모델 도입 시 currentUserId 기반 페이지 편집 권한 검증 추가.
         author :: heechoel shin
         date   :: 2026-05-15T09:00:00KST
         ticket :: LAB-24
         */
    }
}

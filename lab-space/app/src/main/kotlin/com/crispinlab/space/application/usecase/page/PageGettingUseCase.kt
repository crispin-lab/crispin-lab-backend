package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.page.PageGetting
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.incoming.page.PageGetting.Result
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageErrorCode
import org.springframework.stereotype.Service

@Service
class PageGettingUseCase(
    private val pageRepository: PageRepository,
    private val transactionProvider: TransactionProvider
) : PageGetting {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .also {
                    it.validate()
                }.toEntity()
                .toResult()
        }

    private fun Request.validate() {
        /*
        todo    :: visibility·권한 모델 도입 시 외부 의존 검증을 둘 자리. 현재는 누구나 조회 가능.
         author :: heechoel shin
         date   :: 2026-05-13T00:00:00KST
         ticket :: LAB-22
         */
    }

    private fun Request.toEntity(): Page =
        pageRepository.findBy(pageId)
            ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)

    private fun Page.toResult(): Result =
        Result(
            pageId = id,
            spaceId = spaceId,
            parentPageId = parentPageId,
            authorId = authorId,
            title = title,
            content = content.raw,
            visibility = visibility.name,
            currentVersion = currentVersion,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

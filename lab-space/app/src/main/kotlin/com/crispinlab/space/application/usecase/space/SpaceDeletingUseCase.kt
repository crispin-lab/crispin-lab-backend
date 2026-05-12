package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceDeleting
import com.crispinlab.space.application.port.incoming.space.SpaceDeleting.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.Space
import org.springframework.stereotype.Service

@Service
class SpaceDeletingUseCase(
    private val spaceRepository: SpaceRepository,
    private val transactionProvider: TransactionProvider
) : SpaceDeleting {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            request
                .also { it.validate() }
                .toEntity()
                .let { spaceRepository.delete(it.id) }
        }
    }

    private fun Request.validate() {
        /*
        todo    :: 외부 의존성이 필요한 검증을 둘 자리. 권한 등이 도입될 때 채운다.
         author :: heechoel shin
         date   :: 2026-05-11T14:04:49KST
         ticket :: LAB-21
         */
    }

    private fun Request.toEntity(): Space =
        spaceRepository.findBy(spaceId)
            ?: throw NotFoundException("스페이스를 찾을 수 없습니다.")
}

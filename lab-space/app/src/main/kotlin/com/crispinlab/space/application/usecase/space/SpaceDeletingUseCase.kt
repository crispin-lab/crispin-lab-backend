package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceDeleting
import com.crispinlab.space.application.port.incoming.space.SpaceDeleting.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import org.springframework.stereotype.Service

@Service
class SpaceDeletingUseCase(
    private val spaceRepository: SpaceRepository,
    private val transactionProvider: TransactionProvider
) : SpaceDeleting {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            spaceRepository.findBy(request.spaceId)
                ?: throw NotFoundException("스페이스를 찾을 수 없습니다.")
            spaceRepository.delete(request.spaceId)
        }
    }
}

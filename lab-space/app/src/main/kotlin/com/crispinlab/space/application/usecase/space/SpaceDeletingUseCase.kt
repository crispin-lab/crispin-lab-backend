package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceDeleting
import com.crispinlab.space.application.port.incoming.space.SpaceDeleting.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceErrorCode
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
                .withdraw()
        }
    }

    private fun Request.validate() {
        if (!auth.isAdmin) {
            throw ForbiddenException(SpaceErrorCode.SPACE_ADMIN_ONLY)
        }
    }

    private fun Request.toEntity(): Space =
        spaceRepository.findBy(spaceId)
            ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)

    private fun Space.withdraw() {
        spaceRepository.delete(id)
    }
}

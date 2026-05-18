package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceGetting
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Request
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Result
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceErrorCode
import org.springframework.stereotype.Service

@Service
class SpaceGettingUseCase(
    private val spaceRepository: SpaceRepository,
    private val transactionProvider: TransactionProvider
) : SpaceGetting {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .also { it.validate() }
                .toEntity()
                .toResult()
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
            ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)

    private fun Space.toResult(): Result =
        Result(
            spaceId = id,
            name = name,
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

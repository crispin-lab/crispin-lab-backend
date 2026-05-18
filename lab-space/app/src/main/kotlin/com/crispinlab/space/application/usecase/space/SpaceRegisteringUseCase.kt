package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Request
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Result
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId
import org.springframework.stereotype.Service

@Service
class SpaceRegisteringUseCase(
    private val spaceRepository: SpaceRepository,
    private val idGenerator: IdGenerator,
    private val transactionProvider: TransactionProvider
) : SpaceRegistering {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .also { it.validate() }
                .toEntity()
                .let { spaceRepository.save(it) }
                .toResult()
        }

    private fun Request.validate() {
        /*
        todo    :: 외부 의존성이 필요한 검증을 둘 자리. 권한·중복 등이 도입될 때 채운다.
         author :: heechoel shin
         date   :: 2026-05-11T14:04:49KST
         ticket :: LAB-21
         */
    }

    private fun Request.toEntity(): Space =
        Space(
            id = SpaceId(idGenerator.next()),
            name = name,
            description = description
        )

    private fun Space.toResult(): Result = Result(spaceId = id)
}

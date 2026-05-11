package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.time.Clock
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceEditing
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Request
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Result
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.Space
import org.springframework.stereotype.Service

@Service
class SpaceEditingUseCase(
    private val spaceRepository: SpaceRepository,
    private val clock: Clock,
    private val transactionProvider: TransactionProvider
) : SpaceEditing {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .toEntity()
                .editWith(request)
                .let { spaceRepository.save(it) }
                .toResult()
        }

    private fun Request.toEntity(): Space =
        spaceRepository.findBy(spaceId)
            ?: throw NotFoundException("스페이스를 찾을 수 없습니다.")

    private fun Space.editWith(request: Request): Space =
        apply {
            update(
                name = request.name,
                description = request.description,
                occurredAt = clock.now()
            )
        }

    private fun Space.toResult(): Result =
        Result(
            spaceId = id.value,
            name = name,
            description = description,
            updatedAt = updatedAt
        )
}

package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceGetting
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Request
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Result
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceVisibilityScope
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.memberSpaceIdsOf
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceErrorCode
import org.springframework.stereotype.Service

@Service
class SpaceGettingUseCase(
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : SpaceGetting {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .toEntity()
                .toResult()
        }

    private fun Request.toEntity(): Space {
        val scope =
            SpaceVisibilityScope.of(viewer, spaceMemberRepository.memberSpaceIdsOf(viewer))
        return spaceRepository
            .findBy(spaceId)
            ?.takeIf { scope.allows(it.visibility, it.id) }
            ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)
    }

    private fun Space.toResult(): Result =
        Result(
            spaceId = id,
            name = name,
            description = description,
            visibility = visibility,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

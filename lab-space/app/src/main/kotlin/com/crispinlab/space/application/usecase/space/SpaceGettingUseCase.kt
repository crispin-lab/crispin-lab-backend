package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceGetting
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Request
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Result
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceErrorCode
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.user.domain.user.AuthContext
import org.springframework.stereotype.Service

@Service
class SpaceGettingUseCase(
    private val spaceRepository: SpaceRepository,
    private val transactionProvider: TransactionProvider
) : SpaceGetting {
    override fun perform(request: Request): Result =
        transactionProvider.transactional(readOnly = true) {
            request
                .toEntity()
                .toResult()
        }

    private fun Request.toEntity(): Space =
        spaceRepository
            .findBy(spaceId)
            ?.takeIf { it.visibility in auth.allowedSpaceVisibilities() }
            ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)

    private fun AuthContext.allowedSpaceVisibilities(): Set<SpaceVisibility> =
        when (this) {
            is AuthContext.Anonymous -> setOf(SpaceVisibility.PUBLIC)
            is AuthContext.Authenticated -> setOf(SpaceVisibility.PUBLIC, SpaceVisibility.INTERNAL)
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

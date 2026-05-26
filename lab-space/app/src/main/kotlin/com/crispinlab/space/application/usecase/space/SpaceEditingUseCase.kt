package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceEditing
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Request
import com.crispinlab.space.application.port.incoming.space.SpaceEditing.Result
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceErrorCode
import com.crispinlab.space.domain.spacemember.SpaceMemberErrorCode
import org.springframework.stereotype.Service

@Service
class SpaceEditingUseCase(
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : SpaceEditing {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .also { it.validate() }
                .toEntity()
                .editWith(request)
                .let { spaceRepository.save(it) }
                .toResult()
        }

    private fun Request.validate() {
        if (viewer.isAdmin) return
        val membership = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, viewer.userId)
        if (membership?.role?.canManageMembers() != true) {
            throw ForbiddenException(SpaceMemberErrorCode.SPACE_MEMBER_OWNER_ONLY)
        }
    }

    private fun Request.toEntity(): Space =
        spaceRepository.findBy(spaceId)
            ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)

    private fun Space.editWith(request: Request): Space =
        apply {
            edit(
                name = request.name,
                description = request.description,
                visibility = request.visibility
            )
        }

    private fun Space.toResult(): Result =
        Result(
            spaceId = id,
            name = name,
            description = description,
            visibility = visibility,
            updatedAt = updatedAt
        )
}

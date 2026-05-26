package com.crispinlab.space.application.usecase.spacemember

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRoleChanging
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRoleChanging.Request
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRoleChanging.Result
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.spacemember.SpaceMember
import com.crispinlab.space.domain.spacemember.SpaceMemberErrorCode
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import org.springframework.stereotype.Service

@Service
class SpaceMemberRoleChangingUseCase(
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : SpaceMemberRoleChanging {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .also { it.validate() }
                .toEntity()
                .changeRoleWith(request)
                .let { spaceMemberRepository.save(it) }
                .toResult()
        }

    private fun Request.validate() {
        if (viewer.isAdmin) return
        val viewerMembership = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, viewer.userId)
        if (viewerMembership?.role?.canManageMembers() != true) {
            throw ForbiddenException(SpaceMemberErrorCode.SPACE_MEMBER_OWNER_ONLY)
        }
    }

    private fun Request.toEntity(): SpaceMember =
        spaceMemberRepository.findBySpaceIdAndUserId(spaceId, targetUserId)
            ?: throw NotFoundException(SpaceMemberErrorCode.SPACE_MEMBER_NOT_FOUND)

    private fun SpaceMember.changeRoleWith(request: Request): SpaceMember =
        apply {
            if (role == SpaceMemberRole.OWNER &&
                request.role != SpaceMemberRole.OWNER &&
                spaceMemberRepository.lockAndCountOwners(request.spaceId) <= 1
            ) {
                throw ConflictException(SpaceMemberErrorCode.CANNOT_REMOVE_LAST_OWNER)
            }
            changeRole(request.role)
        }

    private fun SpaceMember.toResult(): Result =
        Result(
            spaceMemberId = id,
            spaceId = spaceId,
            userId = userId,
            role = role
        )
}

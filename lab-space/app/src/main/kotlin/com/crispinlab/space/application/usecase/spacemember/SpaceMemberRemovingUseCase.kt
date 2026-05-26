package com.crispinlab.space.application.usecase.spacemember

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRemoving
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRemoving.Request
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.spacemember.SpaceMember
import com.crispinlab.space.domain.spacemember.SpaceMemberErrorCode
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import org.springframework.stereotype.Service

@Service
class SpaceMemberRemovingUseCase(
    private val spaceMemberRepository: SpaceMemberRepository,
    private val transactionProvider: TransactionProvider
) : SpaceMemberRemoving {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            val target =
                request
                    .also { it.validate() }
                    .toEntity()
            if (target.role == SpaceMemberRole.OWNER &&
                spaceMemberRepository.lockAndCountOwners(target.spaceId) <= 1
            ) {
                throw ConflictException(SpaceMemberErrorCode.CANNOT_REMOVE_LAST_OWNER)
            }
            spaceMemberRepository.delete(target.id)
        }
    }

    private fun Request.validate() {
        if (isSelfWithdraw()) return
        if (viewer.isAdmin) return
        val viewerMembership = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, viewer.userId)
        if (viewerMembership?.role?.canManageMembers() != true) {
            throw ForbiddenException(SpaceMemberErrorCode.SPACE_MEMBER_OWNER_ONLY)
        }
    }

    private fun Request.toEntity(): SpaceMember =
        spaceMemberRepository.findBySpaceIdAndUserId(spaceId, targetUserId)
            ?: throw NotFoundException(SpaceMemberErrorCode.SPACE_MEMBER_NOT_FOUND)

    private fun Request.isSelfWithdraw(): Boolean = targetUserId == viewer.userId
}

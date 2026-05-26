package com.crispinlab.space.application.usecase.spacemember

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRemoving
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberRemoving.Request
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.ensureOwnerWillRemain
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
            request
                .also { it.validate() }
                .toEntity()
                .also { target ->
                    spaceMemberRepository.ensureOwnerWillRemain(
                        spaceId = target.spaceId,
                        isLosingOwner = target.role == SpaceMemberRole.OWNER
                    )
                }.let { spaceMemberRepository.delete(it.id) }
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

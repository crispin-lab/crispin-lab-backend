package com.crispinlab.space.application.usecase.spacemember

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberJoining
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberJoining.Request
import com.crispinlab.space.application.port.incoming.spacemember.SpaceMemberJoining.Result
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.space.SpaceErrorCode
import com.crispinlab.space.domain.spacemember.SpaceMember
import com.crispinlab.space.domain.spacemember.SpaceMemberErrorCode
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.user.domain.user.UserId
import java.time.Instant.now
import org.springframework.stereotype.Service

@Service
class SpaceMemberJoiningUseCase(
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val idGenerator: IdGenerator,
    private val transactionProvider: TransactionProvider
) : SpaceMemberJoining {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .also { it.validate() }
                .toEntity()
                .let { spaceMemberRepository.save(it) }
                .toResult()
        }

    private fun Request.validate() {
        spaceRepository.findBy(spaceId)
            ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)
        if (isInvitation()) {
            requireOwner()
        }
        val existing =
            spaceMemberRepository.findBySpaceIdAndUserId(spaceId, effectiveTargetUserId())
        if (existing != null) {
            throw ConflictException(SpaceMemberErrorCode.ALREADY_JOINED)
        }
    }

    private fun Request.toEntity(): SpaceMember =
        SpaceMember(
            id = SpaceMemberId(idGenerator.next()),
            spaceId = spaceId,
            userId = effectiveTargetUserId(),
            role = effectiveRole(),
            joinedAt = now()
        )

    private fun SpaceMember.toResult(): Result =
        Result(
            spaceMemberId = id,
            spaceId = spaceId,
            userId = userId,
            role = role
        )

    private fun Request.isInvitation(): Boolean =
        targetUserId != null && targetUserId != viewer.userId

    private fun Request.effectiveTargetUserId(): UserId = targetUserId ?: viewer.userId

    private fun Request.effectiveRole(): SpaceMemberRole =
        if (isInvitation()) {
            role ?: SpaceMemberRole.MEMBER
        } else {
            SpaceMemberRole.MEMBER
        }

    private fun Request.requireOwner() {
        if (viewer.isAdmin) return
        val membership = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, viewer.userId)
        if (membership?.role?.canManageMembers() != true) {
            throw ForbiddenException(SpaceMemberErrorCode.SPACE_MEMBER_OWNER_ONLY)
        }
    }
}

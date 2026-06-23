package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceGetting
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Request
import com.crispinlab.space.application.port.incoming.space.SpaceGetting.Result
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceVisibilityScope
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.access.canWrite
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceErrorCode
import com.crispinlab.space.domain.spacemember.SpaceMember
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
                .findMembership()
                .let { membership ->
                    request
                        .toEntity(membership)
                        .toResultFor(request.viewer, membership)
                }
        }

    private fun Request.findMembership(): SpaceMember? {
        val current = viewer
        return if (current is Viewer.Member && !current.isAdmin) {
            spaceMemberRepository.findBySpaceIdAndUserId(spaceId, current.userId)
        } else {
            null
        }
    }

    private fun Request.toEntity(membership: SpaceMember?): Space {
        val memberOfSpaceIds = membership?.let { setOf(spaceId) } ?: emptySet()
        val scope = SpaceVisibilityScope.of(viewer, memberOfSpaceIds)
        return spaceRepository
            .findBy(spaceId)
            ?.takeIf { scope.allows(it.visibility, it.id) }
            ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)
    }

    private fun Space.toResultFor(
        viewer: Viewer,
        membership: SpaceMember?
    ): Result =
        Result(
            spaceId = id,
            name = name,
            description = description,
            visibility = visibility,
            canWrite = viewer.canWrite(membership),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

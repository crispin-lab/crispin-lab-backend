package com.crispinlab.space.application.usecase.visit

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.visit.SpaceVisitRecording
import com.crispinlab.space.application.port.incoming.visit.SpaceVisitRecording.Request
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.space.SpaceVisibilityScope
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.port.outgoing.visit.SpaceVisitRepository
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceErrorCode
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.space.domain.visit.SpaceVisit
import com.crispinlab.space.domain.visit.SpaceVisitId
import java.time.Instant.now
import org.springframework.stereotype.Service

@Service
class SpaceVisitRecordingUseCase(
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val spaceVisitRepository: SpaceVisitRepository,
    private val idGenerator: IdGenerator,
    private val transactionProvider: TransactionProvider
) : SpaceVisitRecording {
    override fun perform(request: Request) {
        transactionProvider.transactional {
            request
                .also { it.validate() }
                .toEntity()
                .let { spaceVisitRepository.save(it) }
        }
    }

    private fun Request.validate() {
        val space =
            spaceRepository.findBy(spaceId)
                ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)
        if (!scopeFor(space).allows(space.visibility, space.id)) {
            throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)
        }
    }

    private fun Request.scopeFor(space: Space): SpaceVisibilityScope {
        if (viewer.isAdmin || space.visibility == SpaceVisibility.PUBLIC) {
            return SpaceVisibilityScope.of(viewer, emptySet())
        }
        val memberOfSpaceIds =
            spaceMemberRepository
                .findBySpaceIdAndUserId(spaceId, viewer.userId)
                ?.let { setOf(spaceId) }
                ?: emptySet()
        return SpaceVisibilityScope.of(viewer, memberOfSpaceIds)
    }

    private fun Request.toEntity(): SpaceVisit =
        SpaceVisit(
            id = SpaceVisitId(idGenerator.next()),
            userId = viewer.userId,
            spaceId = spaceId,
            lastVisitedAt = now()
        )
}

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
import com.crispinlab.space.domain.space.SpaceErrorCode
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
        val membership =
            if (viewer.isAdmin) {
                null
            } else {
                spaceMemberRepository.findBySpaceIdAndUserId(spaceId, viewer.userId)
            }
        val memberOfSpaceIds = membership?.let { setOf(spaceId) } ?: emptySet()
        val scope = SpaceVisibilityScope.of(viewer, memberOfSpaceIds)
        spaceRepository
            .findBy(spaceId)
            ?.takeIf { scope.allows(it.visibility, it.id) }
            ?: throw NotFoundException(SpaceErrorCode.SPACE_NOT_FOUND)
    }

    private fun Request.toEntity(): SpaceVisit =
        SpaceVisit(
            id = SpaceVisitId(idGenerator.next()),
            userId = viewer.userId,
            spaceId = spaceId,
            lastVisitedAt = now()
        )
}

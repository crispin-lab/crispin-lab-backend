package com.crispinlab.space.application.usecase.space

import com.crispinlab.common.id.IdGenerator
import com.crispinlab.common.transaction.TransactionProvider
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Request
import com.crispinlab.space.application.port.incoming.space.SpaceRegistering.Result
import com.crispinlab.space.application.port.outgoing.space.SpaceRepository
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.application.usecase.audit.SpaceAuditRecorder
import com.crispinlab.space.domain.space.Space
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMember
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import java.time.Instant.now
import org.springframework.stereotype.Service

@Service
class SpaceRegisteringUseCase(
    private val spaceRepository: SpaceRepository,
    private val spaceMemberRepository: SpaceMemberRepository,
    private val spaceAuditRecorder: SpaceAuditRecorder,
    private val idGenerator: IdGenerator,
    private val transactionProvider: TransactionProvider
) : SpaceRegistering {
    override fun perform(request: Request): Result =
        transactionProvider.transactional {
            request
                .toEntity()
                .let { spaceRepository.save(it) }
                .also { it.registerOwner(request) }
                .also { spaceAuditRecorder.recordRegistered(it, request.viewer) }
                .toResult()
        }

    private fun Request.toEntity(): Space =
        Space(
            id = SpaceId(idGenerator.next()),
            name = name,
            description = description,
            visibility = visibility
        )

    private fun Space.registerOwner(request: Request) {
        spaceMemberRepository.save(
            SpaceMember(
                id = SpaceMemberId(idGenerator.next()),
                spaceId = id,
                userId = request.viewer.userId,
                role = SpaceMemberRole.OWNER,
                joinedAt = now()
            )
        )
    }

    private fun Space.toResult(): Result = Result(spaceId = id)
}

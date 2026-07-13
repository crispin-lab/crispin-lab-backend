package com.crispinlab.composition.adapter.space

import com.crispinlab.composition.application.port.outgoing.space.SpaceVisitLookup
import com.crispinlab.space.application.port.outgoing.visit.SpaceVisitRepository
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant
import org.springframework.stereotype.Component

@Component
class SpaceVisitLookupAdapter(
    private val spaceVisitRepository: SpaceVisitRepository
) : SpaceVisitLookup {
    override fun lastVisitedAtOf(
        userId: UserId,
        spaceIds: Collection<SpaceId>
    ): Map<SpaceId, Instant> {
        val idSet = spaceIds.toSet()
        if (idSet.isEmpty()) return emptyMap()
        return spaceVisitRepository
            .findByUserIdAndSpaceIds(userId, idSet)
            .mapValues { it.value.lastVisitedAt }
    }
}

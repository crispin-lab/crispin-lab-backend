package com.crispinlab.composition.application.port.outgoing.space

import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

interface SpaceVisitLookup {
    fun lastVisitedAtOf(
        userId: UserId,
        spaceIds: Collection<SpaceId>
    ): Map<SpaceId, Instant>
}

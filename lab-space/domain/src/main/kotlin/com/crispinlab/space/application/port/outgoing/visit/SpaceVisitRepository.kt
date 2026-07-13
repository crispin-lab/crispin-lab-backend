package com.crispinlab.space.application.port.outgoing.visit

import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.visit.SpaceVisit
import com.crispinlab.user.domain.user.UserId

interface SpaceVisitRepository {
    fun save(entity: SpaceVisit)

    fun findByUserIdAndSpaceIds(
        userId: UserId,
        spaceIds: Collection<SpaceId>
    ): Map<SpaceId, SpaceVisit>
}

package com.crispinlab.space.domain.visit

import com.crispinlab.common.domain.Entity
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

class SpaceVisit(
    override val id: SpaceVisitId,
    val userId: UserId,
    val spaceId: SpaceId,
    val lastVisitedAt: Instant
) : Entity<SpaceVisitId>

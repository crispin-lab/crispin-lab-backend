package com.crispinlab.composition.application.port.outgoing.space

import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId

interface SpaceMembershipLookup {
    fun membershipsOf(userIds: Collection<UserId>): Map<UserId, Set<SpaceId>>
}

package com.crispinlab.composition.application.port.outgoing.space

import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId

interface SpaceMembershipLookup {
    fun membershipsOf(
        userIds: Collection<UserId>,
        viewer: Viewer
    ): Map<UserId, Set<SpaceId>>

    fun memberIdsIn(
        spaceId: SpaceId,
        userIds: Collection<UserId>,
        viewer: Viewer
    ): Set<UserId>?
}

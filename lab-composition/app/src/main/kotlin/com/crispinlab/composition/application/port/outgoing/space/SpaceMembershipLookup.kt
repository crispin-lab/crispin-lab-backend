package com.crispinlab.composition.application.port.outgoing.space

import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
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

    fun rolesOf(
        userId: UserId,
        spaceIds: Collection<SpaceId>
    ): Map<SpaceId, SpaceMemberRole>

    fun memberCountsOf(spaceIds: Collection<SpaceId>): Map<SpaceId, Long>

    fun memberSpaceIdsOf(viewer: Viewer): Set<SpaceId>
}

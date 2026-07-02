package com.crispinlab.composition.adapter.space

import com.crispinlab.composition.application.port.outgoing.space.SpaceMembershipLookup
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Component

@Component
class SpaceMembershipLookupAdapter(
    private val spaceMemberRepository: SpaceMemberRepository
) : SpaceMembershipLookup {
    override fun membershipsOf(
        userIds: Collection<UserId>,
        viewer: Viewer
    ): Map<UserId, Set<SpaceId>> {
        val idSet = userIds.toSet()
        if (idSet.isEmpty()) return emptyMap()
        val allMemberships = spaceMemberRepository.findSpaceIdsByUserIds(idSet)
        if (viewer.isAdmin) return allMemberships
        val visibleToViewer = viewer.memberSpaceIds()
        return allMemberships.mapValues { (_, spaceIds) ->
            spaceIds intersect visibleToViewer
        }
    }

    private fun Viewer.memberSpaceIds(): Set<SpaceId> =
        when (this) {
            is Viewer.Member -> spaceMemberRepository.findSpaceIdsByUserId(userId)
            Viewer.Anonymous -> emptySet()
        }
}

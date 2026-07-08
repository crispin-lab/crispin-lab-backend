package com.crispinlab.composition.adapter.space

import com.crispinlab.composition.application.port.outgoing.space.SpaceMembershipLookup
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
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

    override fun memberIdsIn(
        spaceId: SpaceId,
        userIds: Collection<UserId>,
        viewer: Viewer
    ): Set<UserId>? {
        val idSet = userIds.toSet()
        if (idSet.isEmpty()) return emptySet()
        if (!viewer.canSeeMembersOf(spaceId)) return null
        val memberships = spaceMemberRepository.findSpaceIdsByUserIds(idSet)
        return idSet.filterTo(mutableSetOf()) { spaceId in memberships[it].orEmpty() }
    }

    override fun rolesOf(
        userId: UserId,
        spaceIds: Collection<SpaceId>
    ): Map<SpaceId, SpaceMemberRole> {
        val idSet = spaceIds.toSet()
        if (idSet.isEmpty()) return emptyMap()
        return spaceMemberRepository.rolesOf(userId, idSet)
    }

    override fun memberCountsOf(spaceIds: Collection<SpaceId>): Map<SpaceId, Long> {
        val idSet = spaceIds.toSet()
        if (idSet.isEmpty()) return emptyMap()
        return spaceMemberRepository.memberCountsOf(idSet)
    }

    override fun memberSpaceIdsOf(viewer: Viewer): Set<SpaceId> = viewer.memberSpaceIds()

    private fun Viewer.memberSpaceIds(): Set<SpaceId> =
        when (this) {
            is Viewer.Member -> spaceMemberRepository.findSpaceIdsByUserId(userId)
            Viewer.Anonymous -> emptySet()
        }

    private fun Viewer.canSeeMembersOf(spaceId: SpaceId): Boolean =
        when (this) {
            is Viewer.Member -> isAdmin || isMemberOf(spaceId)
            Viewer.Anonymous -> false
        }

    private fun Viewer.Member.isMemberOf(spaceId: SpaceId): Boolean =
        spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId) != null
}

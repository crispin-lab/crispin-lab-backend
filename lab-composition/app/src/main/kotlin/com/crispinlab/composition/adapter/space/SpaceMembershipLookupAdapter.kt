package com.crispinlab.composition.adapter.space

import com.crispinlab.composition.application.port.outgoing.space.SpaceMembershipLookup
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Component

@Component
class SpaceMembershipLookupAdapter(
    private val spaceMemberRepository: SpaceMemberRepository
) : SpaceMembershipLookup {
    override fun membershipsOf(userIds: Collection<UserId>): Map<UserId, Set<SpaceId>> {
        val idSet = userIds.toSet()
        if (idSet.isEmpty()) return emptyMap()
        return spaceMemberRepository.findSpaceIdsByUserIds(idSet)
    }
}

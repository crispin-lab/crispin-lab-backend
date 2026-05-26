package com.crispinlab.space.domain.spacemember

import com.crispinlab.common.domain.Entity
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.user.domain.user.UserId
import java.time.Instant

class SpaceMember(
    override val id: SpaceMemberId,
    val spaceId: SpaceId,
    val userId: UserId,
    role: SpaceMemberRole,
    val joinedAt: Instant
) : Entity<SpaceMemberId> {
    var role: SpaceMemberRole = role
        private set

    fun changeRole(role: SpaceMemberRole) {
        this.role = role
    }
}

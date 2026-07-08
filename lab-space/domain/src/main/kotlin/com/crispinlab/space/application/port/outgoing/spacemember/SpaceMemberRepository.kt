package com.crispinlab.space.application.port.outgoing.spacemember

import com.crispinlab.common.pagination.PageRequest
import com.crispinlab.common.pagination.PageResult
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMember
import com.crispinlab.space.domain.spacemember.SpaceMemberId
import com.crispinlab.space.domain.spacemember.SpaceMemberRole
import com.crispinlab.user.domain.user.UserId

interface SpaceMemberRepository {
    fun save(entity: SpaceMember): SpaceMember

    fun findBy(id: SpaceMemberId): SpaceMember?

    fun findBySpaceIdAndUserId(
        spaceId: SpaceId,
        userId: UserId
    ): SpaceMember?

    fun findBySpaceId(
        spaceId: SpaceId,
        pageRequest: PageRequest
    ): PageResult<SpaceMember>

    fun findSpaceIdsByUserId(userId: UserId): Set<SpaceId>

    fun findSpaceIdsByUserIds(userIds: Collection<UserId>): Map<UserId, Set<SpaceId>>

    fun rolesOf(
        userId: UserId,
        spaceIds: Collection<SpaceId>
    ): Map<SpaceId, SpaceMemberRole>

    fun memberCountsOf(spaceIds: Collection<SpaceId>): Map<SpaceId, Long>

    fun countOwnersBy(spaceId: SpaceId): Long

    fun delete(id: SpaceMemberId)
}

package com.crispinlab.space.application.port.outgoing.space

import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.space.SpaceVisibility
import com.crispinlab.user.domain.user.UserId

sealed interface SpaceVisibilityScope {
    fun allows(
        visibility: SpaceVisibility,
        spaceId: SpaceId
    ): Boolean

    data object Anonymous : SpaceVisibilityScope {
        override fun allows(
            visibility: SpaceVisibility,
            spaceId: SpaceId
        ): Boolean = visibility == SpaceVisibility.PUBLIC
    }

    data class Authenticated(
        val viewerId: UserId,
        val memberOfSpaceIds: Set<SpaceId>
    ) : SpaceVisibilityScope {
        override fun allows(
            visibility: SpaceVisibility,
            spaceId: SpaceId
        ): Boolean =
            when (visibility) {
                SpaceVisibility.PUBLIC -> true
                SpaceVisibility.INTERNAL -> spaceId in memberOfSpaceIds
            }
    }

    data object Privileged : SpaceVisibilityScope {
        override fun allows(
            visibility: SpaceVisibility,
            spaceId: SpaceId
        ): Boolean = true
    }

    companion object {
        fun of(
            viewer: Viewer,
            memberOfSpaceIds: Set<SpaceId>
        ): SpaceVisibilityScope =
            when {
                viewer.isAdmin -> Privileged
                viewer is Viewer.Member -> Authenticated(viewer.userId, memberOfSpaceIds)
                else -> Anonymous
            }
    }
}

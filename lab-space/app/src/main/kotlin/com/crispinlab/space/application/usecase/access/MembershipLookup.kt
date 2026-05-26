package com.crispinlab.space.application.usecase.access

import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId

internal fun SpaceMemberRepository.memberSpaceIdsOf(viewer: Viewer): Set<SpaceId> =
    when {
        viewer.isAdmin -> emptySet()
        viewer is Viewer.Member -> findSpaceIdsByUserId(viewer.userId)
        else -> emptySet()
    }

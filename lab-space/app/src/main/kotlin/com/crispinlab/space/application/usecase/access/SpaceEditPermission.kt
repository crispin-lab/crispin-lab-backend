package com.crispinlab.space.application.usecase.access

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMember
import com.crispinlab.space.domain.spacemember.SpaceMemberErrorCode

internal fun Viewer.canEditSpace(membership: SpaceMember?): Boolean =
    when {
        isAdmin -> {
            true
        }

        this is Viewer.Member -> {
            membership?.role?.canManageMembers() == true
        }

        else -> {
            false
        }
    }

internal fun SpaceMemberRepository.canEditSpace(
    viewer: Viewer,
    spaceId: SpaceId
): Boolean =
    when {
        viewer.isAdmin -> {
            true
        }

        viewer is Viewer.Member -> {
            viewer.canEditSpace(findBySpaceIdAndUserId(spaceId, viewer.userId))
        }

        else -> {
            false
        }
    }

internal fun SpaceMemberRepository.requireSpaceEditPermission(
    viewer: Viewer.Member,
    spaceId: SpaceId
) {
    if (!canEditSpace(viewer, spaceId)) {
        throw ForbiddenException(SpaceMemberErrorCode.SPACE_MEMBER_OWNER_ONLY)
    }
}

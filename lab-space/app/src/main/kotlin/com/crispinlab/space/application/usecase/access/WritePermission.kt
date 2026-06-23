package com.crispinlab.space.application.usecase.access

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMember
import com.crispinlab.space.domain.spacemember.SpaceMemberErrorCode

internal fun Viewer.canWrite(membership: SpaceMember?): Boolean =
    when {
        isAdmin -> {
            true
        }

        this is Viewer.Member -> {
            membership?.role?.canWrite() == true
        }

        else -> {
            false
        }
    }

internal fun SpaceMemberRepository.canWrite(
    viewer: Viewer,
    spaceId: SpaceId
): Boolean =
    when {
        viewer.isAdmin -> {
            true
        }

        viewer is Viewer.Member -> {
            viewer.canWrite(findBySpaceIdAndUserId(spaceId, viewer.userId))
        }

        else -> {
            false
        }
    }

internal fun SpaceMemberRepository.requireWritePermission(
    viewer: Viewer.Member,
    spaceId: SpaceId
) {
    if (!canWrite(viewer, spaceId)) {
        throw ForbiddenException(SpaceMemberErrorCode.SPACE_MEMBER_WRITE_DENIED)
    }
}

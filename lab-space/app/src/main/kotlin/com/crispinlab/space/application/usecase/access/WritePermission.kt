package com.crispinlab.space.application.usecase.access

import com.crispinlab.common.exception.ForbiddenException
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.access.Viewer
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberErrorCode

internal fun SpaceMemberRepository.requireWritePermission(
    viewer: Viewer.Member,
    spaceId: SpaceId
) {
    if (viewer.isAdmin) return
    val membership = findBySpaceIdAndUserId(spaceId, viewer.userId)
    if (membership?.role?.canWrite() != true) {
        throw ForbiddenException(SpaceMemberErrorCode.SPACE_MEMBER_WRITE_DENIED)
    }
}

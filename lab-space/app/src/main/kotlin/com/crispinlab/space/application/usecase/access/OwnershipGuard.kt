package com.crispinlab.space.application.usecase.access

import com.crispinlab.common.exception.ConflictException
import com.crispinlab.space.application.port.outgoing.spacemember.SpaceMemberRepository
import com.crispinlab.space.domain.space.SpaceId
import com.crispinlab.space.domain.spacemember.SpaceMemberErrorCode

internal fun SpaceMemberRepository.ensureOwnerWillRemain(
    spaceId: SpaceId,
    isLosingOwner: Boolean
) {
    if (!isLosingOwner) return
    if (countOwnersBy(spaceId) <= 1) {
        throw ConflictException(SpaceMemberErrorCode.CANNOT_REMOVE_LAST_OWNER)
    }
}

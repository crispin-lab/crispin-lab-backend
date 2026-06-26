package com.crispinlab.space.application.usecase.access

import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import com.crispinlab.user.domain.user.UserId

internal fun UserHandleQuery.handleOrEmpty(authorId: UserId): String =
    handlesOf(setOf(authorId))[authorId]?.value ?: ""

internal fun UserHandleQuery.handlesOrEmpty(authorIds: Collection<UserId>): Map<UserId, String> {
    val present = handlesOf(authorIds.toSet())
    return authorIds.associateWith { present[it]?.value ?: "" }
}

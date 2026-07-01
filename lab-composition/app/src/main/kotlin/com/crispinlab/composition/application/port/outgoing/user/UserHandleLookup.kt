package com.crispinlab.composition.application.port.outgoing.user

import com.crispinlab.user.domain.user.UserId

interface UserHandleLookup {
    fun handlesOf(ids: Collection<UserId>): Map<UserId, String>
}

fun UserHandleLookup.handleOf(id: UserId): String = handlesOf(setOf(id))[id] ?: ""

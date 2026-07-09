package com.crispinlab.composition.application.port.outgoing.user

import com.crispinlab.user.domain.user.UserId

interface UserAdminLookup {
    fun adminsAmong(userIds: Collection<UserId>): Set<UserId>
}

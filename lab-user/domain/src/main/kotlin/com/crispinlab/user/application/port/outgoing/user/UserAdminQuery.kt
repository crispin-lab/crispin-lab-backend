package com.crispinlab.user.application.port.outgoing.user

import com.crispinlab.user.domain.user.UserId

interface UserAdminQuery {
    fun adminsAmong(ids: Collection<UserId>): Set<UserId>
}

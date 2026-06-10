package com.crispinlab.user.application.port.outgoing.user

import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId

interface UserHandleQuery {
    fun handlesOf(ids: Collection<UserId>): Map<UserId, Handle>
}

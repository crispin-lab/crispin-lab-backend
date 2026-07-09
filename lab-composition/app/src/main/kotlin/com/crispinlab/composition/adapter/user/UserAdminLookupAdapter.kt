package com.crispinlab.composition.adapter.user

import com.crispinlab.composition.application.port.outgoing.user.UserAdminLookup
import com.crispinlab.user.application.port.outgoing.user.UserAdminQuery
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Component

@Component
class UserAdminLookupAdapter(
    private val userAdminQuery: UserAdminQuery
) : UserAdminLookup {
    override fun adminsAmong(userIds: Collection<UserId>): Set<UserId> {
        val idSet = userIds.toSet()
        if (idSet.isEmpty()) return emptySet()
        return userAdminQuery.adminsAmong(idSet)
    }
}

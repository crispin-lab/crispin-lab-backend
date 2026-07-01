package com.crispinlab.composition.adapter.user

import com.crispinlab.composition.application.port.outgoing.user.UserHandleLookup
import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import com.crispinlab.user.domain.user.UserId
import org.springframework.stereotype.Component

@Component
class UserHandleLookupAdapter(
    private val userHandleQuery: UserHandleQuery
) : UserHandleLookup {
    override fun handlesOf(ids: Collection<UserId>): Map<UserId, String> {
        val idSet = ids.toSet()
        if (idSet.isEmpty()) return emptyMap()
        val present = userHandleQuery.handlesOf(idSet)
        return idSet.associateWith { present[it]?.value ?: "" }
    }
}

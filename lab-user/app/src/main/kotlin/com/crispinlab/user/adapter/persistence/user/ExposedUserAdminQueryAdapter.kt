package com.crispinlab.user.adapter.persistence.user

import com.crispinlab.user.application.port.outgoing.user.UserAdminQuery
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository

@Repository
class ExposedUserAdminQueryAdapter : UserAdminQuery {
    override fun adminsAmong(ids: Collection<UserId>): Set<UserId> {
        if (ids.isEmpty()) return emptySet()
        val rawIds = ids.map { it.value }.distinct()
        return Users
            .select(Users.id)
            .where {
                (Users.id inList rawIds) and
                    (Users.role eq SystemRole.ADMIN.name) and
                    Users.deletedAt.isNull()
            }.map { UserId(it[Users.id]) }
            .toSet()
    }
}

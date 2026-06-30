package com.crispinlab.user.adapter.search.user

import com.crispinlab.common.persistence.escapeLike
import com.crispinlab.user.adapter.persistence.user.UserHandleDecoder
import com.crispinlab.user.adapter.persistence.user.Users
import com.crispinlab.user.application.port.outgoing.user.UserSearchPort
import com.crispinlab.user.application.port.outgoing.user.UserSearchPort.Match
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository

@Repository
class ExposedUserSearchAdapter : UserSearchPort {
    override fun search(
        query: String,
        size: Int
    ): List<Match> {
        val pattern = "%${query.lowercase().escapeLike()}%"
        return Users
            .select(Users.id, Users.handle)
            .where {
                (Users.handle.lowerCase() like pattern) and Users.deletedAt.isNull()
            }.orderBy(Users.handle to SortOrder.ASC, Users.id to SortOrder.ASC)
            .limit(size)
            .mapNotNull { it.toMatch() }
    }

    private fun ResultRow.toMatch(): Match? {
        val userId = UserId(this[Users.id])
        return UserHandleDecoder.decode(this[Users.handle], userId)?.let {
            Match(userId = userId, handle = it)
        }
    }
}

package com.crispinlab.user.adapter.persistence.user

import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository

@Repository
class ExposedUserHandleQueryAdapter : UserHandleQuery {
    override fun handlesOf(ids: Collection<UserId>): Map<UserId, Handle> {
        if (ids.isEmpty()) return emptyMap()
        val rawIds = ids.map { it.value }.distinct()
        return Users
            .select(Users.id, Users.handle)
            .where { (Users.id inList rawIds) and Users.deletedAt.isNull() }
            .associate { UserId(it[Users.id]) to decodeHandle(it[Users.handle]) }
    }

    private fun decodeHandle(stored: String): Handle =
        runCatching { Handle(stored) }
            .getOrElse { cause ->
                throw IllegalStateException("저장된 handle 값을 해석할 수 없습니다.", cause)
            }
}

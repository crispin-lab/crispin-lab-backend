package com.crispinlab.user.adapter.persistence.user

import com.crispinlab.user.application.port.outgoing.user.UserHandleQuery
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.UserId
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.select
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class ExposedUserHandleQueryAdapter : UserHandleQuery {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handlesOf(ids: Collection<UserId>): Map<UserId, Handle> {
        if (ids.isEmpty()) return emptyMap()
        val rawIds = ids.map { it.value }.distinct()
        return Users
            .select(Users.id, Users.handle)
            .where { (Users.id inList rawIds) and Users.deletedAt.isNull() }
            .mapNotNull { row ->
                val userId = UserId(row[Users.id])
                decodeHandle(row[Users.handle], userId)?.let { userId to it }
            }.toMap()
    }

    private fun decodeHandle(
        stored: String,
        userId: UserId
    ): Handle? =
        runCatching { Handle(stored) }
            .onFailure {
                log.warn(
                    "저장된 handle 값을 해석할 수 없습니다 — skip. userId={}",
                    userId.value
                )
            }.getOrNull()
}

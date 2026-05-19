package com.crispinlab.user.application.port.outgoing.user

import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.User
import com.crispinlab.user.domain.user.UserId

interface UserRepository {
    fun save(entity: User): User

    fun findBy(id: UserId): User?

    fun findByEmail(email: EmailAddress): User?

    fun existsByEmail(email: EmailAddress): Boolean

    fun existsByHandle(handle: Handle): Boolean

    fun delete(id: UserId)
}

package com.crispinlab.user.application.port.outgoing.user

import com.crispinlab.user.domain.user.User
import com.crispinlab.user.domain.user.UserId

interface UserRepository {
    fun save(user: User): User

    fun findBy(id: UserId): User?

    fun delete(id: UserId)
}

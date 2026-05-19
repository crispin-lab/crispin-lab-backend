package com.crispinlab.user.application.port.outgoing.credential

import com.crispinlab.user.domain.credential.UserCredential
import com.crispinlab.user.domain.credential.UserCredentialId
import com.crispinlab.user.domain.user.UserId

interface UserCredentialRepository {
    fun save(entity: UserCredential): UserCredential

    fun findBy(id: UserCredentialId): UserCredential?

    fun findPasswordBy(userId: UserId): UserCredential?

    fun delete(id: UserCredentialId)
}

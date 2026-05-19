package com.crispinlab.user.testsupport

import com.crispinlab.user.domain.credential.Credential
import com.crispinlab.user.domain.credential.PasswordHash
import com.crispinlab.user.domain.credential.UserCredential
import com.crispinlab.user.domain.credential.UserCredentialId
import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.Handle
import com.crispinlab.user.domain.user.SystemRole
import com.crispinlab.user.domain.user.User
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.Dummies.DUMMY_INSTANT
import java.time.Instant

object Fixtures {
    fun basicUser(
        id: UserId = UserId(1L),
        email: EmailAddress = EmailAddress("user@example.com"),
        handle: Handle = Handle("test_user"),
        role: SystemRole = SystemRole.USER,
        createdAt: Instant = DUMMY_INSTANT,
        updatedAt: Instant = createdAt,
        deletedAt: Instant? = null
    ): User =
        User(
            id = id,
            email = email,
            handle = handle,
            role = role,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt
        )

    fun basicUserCredential(
        id: UserCredentialId = UserCredentialId(10L),
        userId: UserId = UserId(1L),
        credential: Credential = Credential.Password(PasswordHash("\$2a\$12\$" + "a".repeat(53))),
        createdAt: Instant = DUMMY_INSTANT,
        updatedAt: Instant = createdAt
    ): UserCredential =
        UserCredential(
            id = id,
            userId = userId,
            credential = credential,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}

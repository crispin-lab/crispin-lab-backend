package com.crispinlab.user.testsupport

import com.crispinlab.user.domain.user.EmailAddress
import com.crispinlab.user.domain.user.User
import com.crispinlab.user.domain.user.UserId
import com.crispinlab.user.testsupport.Dummies.DUMMY_INSTANT
import java.time.Instant

object Fixtures {
    fun basicUser(
        id: UserId = UserId(1L),
        email: EmailAddress = EmailAddress("user@example.com"),
        displayName: String = "테스트 사용자",
        createdAt: Instant = DUMMY_INSTANT
    ): User =
        User(
            id = id,
            email = email,
            displayName = displayName,
            createdAt = createdAt
        )
}

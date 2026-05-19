package com.crispinlab.user.domain.credential

import com.crispinlab.common.domain.Entity
import com.crispinlab.user.domain.user.UserId
import java.time.Instant
import java.time.Instant.now

class UserCredential(
    override val id: UserCredentialId,
    val userId: UserId,
    credential: Credential,
    val createdAt: Instant = now(),
    updatedAt: Instant = createdAt
) : Entity<UserCredentialId> {
    var credential: Credential = credential
        private set
    var updatedAt: Instant = updatedAt
        private set

    fun rotate(credential: Credential) {
        check(credential::class == this.credential::class) {
            "자격증명 종류는 변경할 수 없습니다 — 별도 entity 로 재발급해야 합니다."
        }
        this.credential = credential
        updatedAt = now()
    }
}

package com.crispinlab.user.domain.user

import com.crispinlab.common.domain.Entity
import com.crispinlab.common.domain.SoftDeletable
import java.time.Instant
import java.time.Instant.now

class User(
    override val id: UserId,
    val email: EmailAddress,
    handle: Handle,
    role: SystemRole = SystemRole.USER,
    val createdAt: Instant = now(),
    updatedAt: Instant = createdAt,
    deletedAt: Instant? = null
) : Entity<UserId>,
    SoftDeletable {
    var handle: Handle = handle
        private set
    var role: SystemRole = role
        private set
    var updatedAt: Instant = updatedAt
        private set
    override var deletedAt: Instant? = deletedAt
        private set

    fun changeHandle(handle: Handle) {
        check(!isDeleted) {
            "삭제된 사용자는 수정할 수 없습니다."
        }
        this.handle = handle
        updatedAt = now()
    }

    fun promoteTo(role: SystemRole) {
        check(!isDeleted) {
            "삭제된 사용자는 수정할 수 없습니다."
        }
        this.role = role
        updatedAt = now()
    }
}

package com.crispinlab.user.domain.credential

import com.crispinlab.common.domain.EntityId

data class UserCredentialId(
    override val value: Long
) : EntityId {
    companion object {
        fun String.asUserCredentialId(): UserCredentialId =
            UserCredentialId(
                toLongOrNull()
                    ?: throw IllegalArgumentException("자격증명 ID 형식이 올바르지 않습니다.")
            )
    }
}

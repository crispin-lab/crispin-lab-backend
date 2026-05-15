package com.crispinlab.user.domain.user

import java.time.Instant
import java.time.Instant.now

class User(
    val id: UserId,
    val email: EmailAddress,
    displayName: String,
    val createdAt: Instant = now(),
    updatedAt: Instant = createdAt
) {
    var displayName: String = displayName
        private set
    var updatedAt: Instant = updatedAt
        private set

    init {
        validateDisplayName(displayName)
    }

    fun edit(displayName: String? = null) {
        displayName?.also {
            validateDisplayName(it)
            this.displayName = it
        }
        updatedAt = now()
    }

    private fun validateDisplayName(displayName: String) {
        require(displayName.isNotBlank()) {
            "표시 이름을 입력해 주세요."
        }
        require(displayName.length <= MAX_DISPLAY_NAME_LENGTH) {
            "표시 이름은 ${MAX_DISPLAY_NAME_LENGTH}자를 넘을 수 없습니다."
        }
    }

    companion object {
        const val MAX_DISPLAY_NAME_LENGTH: Int = 50
    }
}

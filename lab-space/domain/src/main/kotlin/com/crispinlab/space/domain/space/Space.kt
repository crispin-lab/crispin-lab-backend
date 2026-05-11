package com.crispinlab.space.domain.space

import java.time.Instant
import java.time.Instant.now

class Space(
    val id: SpaceId,
    name: String,
    description: String,
    val createdAt: Instant = now(),
    updatedAt: Instant = createdAt
) {
    var name: String = name
        private set
    var description: String = description
        private set
    var updatedAt: Instant = updatedAt
        private set

    init {
        validateName(name)
        validateDescription(description)
    }

    fun edit(
        name: String? = null,
        description: String? = null
    ) {
        var changed = false
        name?.also {
            validateName(it)
            this.name = it
            changed = true
        }
        description?.also {
            validateDescription(it)
            this.description = it
            changed = true
        }
        if (changed) {
            this.updatedAt = now()
        }
    }

    private fun validateName(name: String) {
        require(name.isNotBlank()) {
            "스페이스 이름을 입력해 주세요."
        }
        require(name.length <= MAX_NAME_LENGTH) {
            "스페이스 이름은 ${MAX_NAME_LENGTH}자를 넘을 수 없습니다."
        }
    }

    private fun validateDescription(description: String) {
        require(description.length <= MAX_DESCRIPTION_LENGTH) {
            "스페이스 설명은 ${MAX_DESCRIPTION_LENGTH}자를 넘을 수 없습니다."
        }
    }

    companion object {
        const val MAX_NAME_LENGTH: Int = 100
        const val MAX_DESCRIPTION_LENGTH: Int = 500
    }
}

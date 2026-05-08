package com.crispinlab.space.domain.space

import java.time.Instant

class Space(
    val id: SpaceId,
    name: String,
    description: String,
    val createdAt: Instant,
    updatedAt: Instant = createdAt
) {
    var name: String = name
        private set
    var description: String = description
        private set
    var updatedAt: Instant = updatedAt
        private set

    init {
        require(name.isNotBlank()) {
            "스페이스 이름을 입력해 주세요."
        }
        require(name.length <= MAX_NAME_LENGTH) {
            "스페이스 이름은 ${MAX_NAME_LENGTH}자를 넘을 수 없습니다."
        }
        require(description.length <= MAX_DESCRIPTION_LENGTH) {
            "스페이스 설명은 ${MAX_DESCRIPTION_LENGTH}자를 넘을 수 없습니다."
        }
    }

    fun update(
        name: String? = null,
        description: String? = null,
        occurredAt: Instant
    ) {
        name?.also {
            require(it.isNotBlank()) {
                "스페이스 이름을 입력해 주세요."
            }
            require(it.length <= MAX_NAME_LENGTH) {
                "스페이스 이름은 ${MAX_NAME_LENGTH}자를 넘을 수 없습니다."
            }
            this.name = it
        }
        description?.also {
            require(it.length <= MAX_DESCRIPTION_LENGTH) {
                "스페이스 설명은 ${MAX_DESCRIPTION_LENGTH}자를 넘을 수 없습니다."
            }
            this.description = it
        }
        this.updatedAt = occurredAt
    }

    companion object {
        const val MAX_NAME_LENGTH: Int = 100
        const val MAX_DESCRIPTION_LENGTH: Int = 500
    }
}

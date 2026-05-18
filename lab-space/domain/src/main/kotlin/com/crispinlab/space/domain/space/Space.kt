package com.crispinlab.space.domain.space

import com.crispinlab.common.domain.Entity
import com.crispinlab.common.domain.SoftDeletable
import java.time.Instant
import java.time.Instant.now

class Space(
    override val id: SpaceId,
    name: String,
    description: String,
    val createdAt: Instant = now(),
    updatedAt: Instant = createdAt,
    deletedAt: Instant? = null
) : Entity<SpaceId>,
    SoftDeletable {
    var name: String = name
        private set
    var description: String = description
        private set
    var updatedAt: Instant = updatedAt
        private set
    override var deletedAt: Instant? = deletedAt
        private set

    init {
        validateName(name)
        validateDescription(description)
    }

    fun edit(
        name: String? = null,
        description: String? = null
    ) {
        check(!isDeleted) {
            "삭제된 스페이스는 수정할 수 없습니다."
        }
        name?.also {
            validateName(it)
            this.name = it
        }
        description?.also {
            validateDescription(it)
            this.description = it
        }
        updatedAt = now()
    }

    fun delete() {
        check(!isDeleted) {
            "이미 삭제된 스페이스입니다."
        }
        val occurredAt: Instant = now()
        this.deletedAt = occurredAt
        this.updatedAt = occurredAt
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

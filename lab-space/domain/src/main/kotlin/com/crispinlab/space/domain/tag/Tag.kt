package com.crispinlab.space.domain.tag

import com.crispinlab.space.domain.space.SpaceId
import java.time.Instant

class Tag(
    val id: TagId,
    val spaceId: SpaceId,
    name: String,
    val createdAt: Instant
) {
    var name: String = name
        private set

    init {
        require(name.matches(NAME_REGEX)) {
            "태그 이름은 1~30자의 문자/숫자/-/_ 만 허용합니다."
        }
    }

    fun rename(name: String) {
        require(name.matches(NAME_REGEX)) {
            "태그 이름은 1~30자의 문자/숫자/-/_ 만 허용합니다."
        }
        this.name = name
    }

    companion object {
        val NAME_REGEX: Regex = Regex("^[\\p{L}\\p{N}_-]{1,30}$")
    }
}

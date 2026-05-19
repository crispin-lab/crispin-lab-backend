package com.crispinlab.user.domain.user

import com.crispinlab.common.domain.StringValue

data class EmailAddress(
    override val value: String
) : StringValue {
    init {
        require(value.isNotBlank()) {
            "이메일을 입력해 주세요."
        }
        require(value.length <= MAX_LENGTH) {
            "이메일은 ${MAX_LENGTH}자를 넘을 수 없습니다."
        }
        require(EMAIL_REGEX.matches(value)) {
            "이메일 형식이 올바르지 않습니다."
        }
    }

    companion object {
        const val MAX_LENGTH: Int = 254
        private val EMAIL_REGEX: Regex = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")
    }
}

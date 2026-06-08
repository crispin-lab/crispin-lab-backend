package com.crispinlab.user.domain.user

import com.crispinlab.common.domain.StringValue

data class Handle(
    override val value: String
) : StringValue {
    init {
        require(HANDLE_REGEX.matches(value)) {
            "사용자 이름은 영문 소문자, 숫자, 밑줄(_) 로 구성된 ${MIN_LENGTH}~${MAX_LENGTH}자여야 합니다."
        }
    }

    companion object {
        const val MIN_LENGTH: Int = 3
        const val MAX_LENGTH: Int = 30
        private val HANDLE_REGEX: Regex = Regex("""^[a-z0-9_]{$MIN_LENGTH,$MAX_LENGTH}$""")
    }
}

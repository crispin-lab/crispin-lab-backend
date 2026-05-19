package com.crispinlab.user.domain.session

import com.crispinlab.common.domain.StringValue

data class SessionToken(
    override val value: String
) : StringValue {
    init {
        require(value.startsWith(PREFIX)) {
            "세션 토큰 형식이 올바르지 않습니다."
        }
        require(BODY_REGEX.matches(value.removePrefix(PREFIX))) {
            "세션 토큰 형식이 올바르지 않습니다."
        }
    }

    override fun toString(): String = "SessionToken(value=***)"

    companion object {
        const val PREFIX: String = "sess_"
        const val BODY_LENGTH: Int = 43
        private val BODY_REGEX: Regex = Regex("""^[A-Za-z0-9_-]{$BODY_LENGTH}$""")

        fun String.asSessionToken(): SessionToken = SessionToken(this)
    }
}

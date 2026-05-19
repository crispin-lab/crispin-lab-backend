package com.crispinlab.user.domain.credential

sealed class Credential {
    data class Password(
        val hash: PasswordHash
    ) : Credential()

    data class OAuth(
        val provider: OAuthProvider,
        val subjectId: String
    ) : Credential() {
        init {
            require(subjectId.isNotBlank()) {
                "OAuth subject ID 가 비어 있습니다."
            }
            require(subjectId.length <= MAX_SUBJECT_ID_LENGTH) {
                "OAuth subject ID 는 ${MAX_SUBJECT_ID_LENGTH}자를 넘을 수 없습니다."
            }
        }

        companion object {
            const val MAX_SUBJECT_ID_LENGTH: Int = 255
        }
    }
}

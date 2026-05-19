package com.crispinlab.user.domain.user

enum class SystemRole {
    USER,
    ADMIN
    ;

    companion object {
        fun String.asSystemRole(): SystemRole =
            entries.firstOrNull { it.name == uppercase() }
                ?: throw IllegalArgumentException("지원하지 않는 사용자 역할입니다.")
    }
}

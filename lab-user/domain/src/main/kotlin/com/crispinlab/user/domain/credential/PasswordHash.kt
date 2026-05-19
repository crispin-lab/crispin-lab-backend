package com.crispinlab.user.domain.credential

data class PasswordHash(
    val value: String
) {
    init {
        require(value.isNotBlank()) {
            "비밀번호 해시가 비어 있습니다."
        }
    }

    // hash 가 로그·예외 메시지에 자동 노출되지 않게 마스킹. bcrypt 해시는 raw password 가
    // 아니지만 salt/cost 가 유출되면 offline brute force 의 시작점이 된다.
    override fun toString(): String = "PasswordHash(value=***)"
}

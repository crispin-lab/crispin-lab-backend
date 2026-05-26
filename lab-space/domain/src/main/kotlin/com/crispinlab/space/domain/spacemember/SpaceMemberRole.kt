package com.crispinlab.space.domain.spacemember

enum class SpaceMemberRole {
    OWNER,
    MEMBER,
    VIEWER
    ;

    fun canWrite(): Boolean = this != VIEWER

    fun canManageMembers(): Boolean = this == OWNER

    companion object {
        fun String.asSpaceMemberRole(): SpaceMemberRole =
            entries.firstOrNull { it.name == uppercase() }
                ?: throw IllegalArgumentException("지원하지 않는 스페이스 멤버 역할입니다.")
    }
}

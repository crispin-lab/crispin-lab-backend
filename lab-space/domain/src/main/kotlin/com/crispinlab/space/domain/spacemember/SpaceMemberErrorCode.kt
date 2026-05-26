package com.crispinlab.space.domain.spacemember

import com.crispinlab.common.exception.ErrorCode

enum class SpaceMemberErrorCode(
    override val defaultMessage: String
) : ErrorCode {
    SPACE_MEMBER_NOT_FOUND("스페이스 멤버를 찾을 수 없습니다."),
    ALREADY_JOINED("이미 스페이스에 참여하고 있습니다."),
    CANNOT_REMOVE_LAST_OWNER("마지막 소유자는 제거할 수 없습니다."),
    SPACE_MEMBER_OWNER_ONLY("스페이스 소유자만 수행할 수 있습니다."),
    SPACE_MEMBER_WRITE_DENIED("스페이스에 쓰기 권한이 없습니다.")
    ;

    override val code: String get() = name
}

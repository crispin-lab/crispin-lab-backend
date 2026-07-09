package com.crispinlab.space.domain.audit

enum class SpaceAuditAction {
    REGISTERED,
    EDITED,
    DELETED
    ;

    companion object {
        fun String.asSpaceAuditAction(): SpaceAuditAction =
            entries.firstOrNull { it.name == uppercase() }
                ?: throw IllegalArgumentException("지원하지 않는 감사 이력 종류입니다.")
    }
}

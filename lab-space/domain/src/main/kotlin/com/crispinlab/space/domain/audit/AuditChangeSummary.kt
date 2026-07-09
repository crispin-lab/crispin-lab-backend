package com.crispinlab.space.domain.audit

data class AuditChangeSummary(
    val json: String
) {
    init {
        require(json.isNotBlank()) {
            "변경 요약을 입력해 주세요."
        }
        val trimmed = json.trim()
        require(trimmed.startsWith("{") && trimmed.endsWith("}")) {
            "변경 요약은 JSON 객체 형식이어야 합니다."
        }
    }
}

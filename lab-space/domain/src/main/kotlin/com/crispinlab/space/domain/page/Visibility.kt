package com.crispinlab.space.domain.page

/**
 * 항목 선언 순서가 audience 폭. 좁은(0) → 넓은(3).
 * cascade 정책의 effective = min(page.ordinal, space.ceiling().ordinal) 이 본 순서에 결합한다.
 * 항목 추가/이동 시 PageVisibilityScopeSql 의 SQL 분기와 SpaceVisibility.ceiling() 매핑도 같이 갱신.
 */
enum class Visibility {
    DRAFT,
    INTERNAL,
    MEMBER,
    PUBLIC;

    companion object {
        fun String.asVisibility(): Visibility =
            entries.firstOrNull { it.name == uppercase() }
                ?: throw IllegalArgumentException("지원하지 않는 공개 범위입니다.")
    }
}

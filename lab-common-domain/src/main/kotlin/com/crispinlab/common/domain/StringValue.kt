package com.crispinlab.common.domain

/**
 * String 단일 값을 감싸는 도메인 값 객체의 공통 super type.
 *
 * `Handle` / `EmailAddress` 등 String 기반 value object 가 같은 직렬화 정책 (JSON 에서 평문 String
 * 으로 노출) 에 묶일 enabler. 외부 응답에 그대로 노출돼선 안 되는 sensitive 값 객체
 * (예: `PasswordHash`) 는 본 마커를 implement 하지 않는다.
 */
interface StringValue {
    val value: String
}

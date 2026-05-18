package com.crispinlab.common.domain

/**
 * Long 단일 값을 감싸는 도메인 값 객체의 공통 super type.
 *
 * `EntityId` 와 미래의 `Money` / `Score` 같은 Long 기반 값 객체가 같은 직렬화 정책 (예: JSON 에서
 * String 으로 노출 — snowflake 64-bit Long 의 JS `Number.MAX_SAFE_INTEGER` 초과 정밀도 손실 방지)
 * 에 묶일 enabler. 자세한 의도는 `entity.md` 참조.
 */
interface LongValue {
    val value: Long
}

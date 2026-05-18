package com.crispinlab.common.domain

/**
 * 도메인 entity 의 식별자 마커.
 *
 * `lab-common-infra` 의 `EntityIdJacksonConfiguration` 이 이 타입을 대상으로 JSON 직렬화 정책을
 * 적용한다 (super-type dispatch 로 모든 구현체에 동일 정책). 자세한 정책 근거는 `entity.md` 참조.
 */
interface EntityId : LongValue

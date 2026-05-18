package com.crispinlab.common.domain

/**
 * 도메인 entity 의 공통 super type.
 *
 * 자체 동작은 없고, 인프라 어댑터 (`ExposedEntityRepository<E : Entity<I>, I : EntityId>` 등) 가
 * entity 의 ID 에 일관 접근할 수 있게 하는 generic enabler 역할. 자세한 적용 룰은 `entity.md` 참조.
 */
interface Entity<ID : EntityId> {
    val id: ID
}

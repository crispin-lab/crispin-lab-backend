package com.crispinlab.common.domain

import java.time.Instant

/**
 * Soft delete 정책을 따르는 entity 의 mixin 마커.
 *
 * 자체 동작은 없고, 인프라 어댑터 (`ExposedEntityRepository`) 가 `deleted_at` 컬럼을 일관 매핑·자동 필터하기
 * 위한 신호. 표준 삭제 흐름은 UseCase 의 `repository.delete(id)` (`usecase-implementation.md` "Deleting").
 * `delete()` 같은 상태 전이는 각 entity 가 자기 invariant 와 함께 구현 가능한 enabler — 현 시점 호출처가 없을 수 있다.
 *
 * `EntityId` / `Entity<ID>` 와 직교 — entity 는 양쪽을 함께 implement
 * (`Page : Entity<PageId>, SoftDeletable`). 적용 룰은 `entity.md` 참조.
 */
interface SoftDeletable {
    val deletedAt: Instant?

    val isDeleted: Boolean
        get() = deletedAt != null
}

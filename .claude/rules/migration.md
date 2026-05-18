# DB 마이그레이션

> **이 문서의 범위**: Flyway 기반 SQL 마이그레이션 파일의 위치·네이밍·작성 규약과 Postgres + Testcontainers 로 회귀를 잡는 테스트 전략.
>
> **Repository / Exposed 테이블**: `repository.md`
> **로컬 인프라 (Postgres compose)**: `dev-infra.md`
> **모듈 경계**: `architecture.md`

## 한 줄 요약

- 마이그레이션 SQL 은 `lab-{domain}/app/src/main/resources/db/migration/` 에 둔다.
- 파일명은 `V{YYYYMMDDHHmmss}__{snake_case_summary}.sql`. 도메인이 여럿일 땐 description 앞에 `{domain}_` prefix.
- forward only. 백필·롤백 SQL 은 별도 티켓.
- Repository 테스트는 Postgres Testcontainer + Flyway 가 실제 마이그레이션을 적용한 상태에서 돈다 — SQL 정합성은 자동 회귀.

## 위치

```
lab-{domain}/app/src/main/resources/db/migration/V{ts}__{summary}.sql
```

- 모듈 분리 원칙: 마이그레이션은 그 스키마를 사용하는 도메인 모듈 안에 둔다. Flyway 가 classpath 전체에서 `db/migration/*.sql` 을 수집하므로 모듈 위치는 자유이지만, **도메인 응집을 위해 entity / Exposed 테이블과 같은 모듈에 둔다.**
- 새 도메인 모듈을 추가하면 동일 경로 (`lab-user/app/src/main/resources/db/migration/`) 에 마이그레이션을 둔다.
- 부팅 가능한 `app` 모듈은 Flyway 를 *실행* 만 한다 (Spring Boot autoconfig). 마이그레이션 SQL 은 두지 않는다.

## 네이밍

```
V20260514120000__lab_space_init.sql
V20260514130000__lab_space_add_comments.sql
V20260520090000__lab_user_init.sql
```

| 요소 | 규칙 | 이유 |
|------|------|------|
| 버전 prefix | `V` | Flyway 의 versioned migration 표기. |
| 타임스탬프 | 14자리 `YYYYMMDDHHmmss` | 멀티 브랜치 동시 작업 시 충돌 회피. `V1/V2` 순차 번호는 PR 머지 순서에 따라 충돌. 날짜만(`YYYYMMDD`) 은 같은 날 2건이 충돌. |
| 구분자 | `__` (언더스코어 2개) | Flyway 표준. |
| description | `snake_case`, 도메인 prefix + 의도 | `lab_space_init`, `lab_space_add_comments` 같은 형태. `lab_<domain>_` prefix 가 정렬·검색에서 도메인 분리에 유리. |
| 확장자 | `.sql` | repeatable 마이그레이션은 현재 사용 안 함. 도입 시 `R__` prefix 별도 룰. |

새 마이그레이션을 만들 때 description 첫 단어는 동작 또는 상태 (`init`, `add_<table>`, `add_<column>_to_<table>`, `drop_<...>`, `rename_<...>`, `backfill_<...>`).

## 작성 규칙

### Postgres 표준 SQL 만

- 본 프로젝트는 Postgres 전용. H2 호환 모드를 고려할 필요 없음.
- `BIGINT`, `INTEGER`, `VARCHAR(N)`, `TEXT`, `TIMESTAMP`, `BOOLEAN` 등 표준 타입만.
- `TIMESTAMP WITH TIME ZONE` 이 필요해지면 그 때 도입 — 현재 Exposed 의 `timestamp()` 가 `TIMESTAMP WITHOUT TIME ZONE` 으로 매핑되므로 `TIMESTAMP` 로 통일.

### Exposed 테이블 정의와 정합

마이그레이션 SQL 이 정의하는 컬럼·타입·길이·인덱스·제약은 `lab-{domain}/app/src/main/kotlin/.../adapter/persistence/{aggregate}/{Aggregate}s.kt` 의 Exposed `Table` 정의와 1:1 로 일치해야 한다.

- `varchar(N)` 의 N 은 entity 의 `MAX_*_LENGTH` 상수와 동일.
- nullable 컬럼은 Exposed 에서 `.nullable()`, SQL 에서 `NULL`.
- index 는 Exposed 에서 `.index()` / `uniqueIndex(...)`, SQL 에서 `CREATE INDEX` / `CREATE UNIQUE INDEX`.
- index 이름 컨벤션:
  - 일반: `{table}_{column}_idx`
  - 복합: `{table}_{col1}_{col2}_idx`
  - unique: `{table}_{cols}_uidx`
- FK 는 현재 두지 않는다 — snowflake ID 기반, 무결성은 application 책임. FK 가 필요해지면 그때 마이그레이션으로 추가 + 정책 결정.

### Drift 체크리스트

필드 추가·변경 PR 에서 마이그레이션과 다른 계층이 어긋나지 않도록 다음을 확인.

- [ ] 컬럼 nullable / NOT NULL 결정이 domain entity 의 nullable 타입(`String?` vs `String`) 과 일치하는가
- [ ] 필수/선택 의미가 entity 의 `require(... isNotBlank())` 또는 빈 문자열 허용 정책과 일관되는가 (NOT NULL 인데 빈 문자열 허용은 위험 신호)
- [ ] `varchar(N)` 의 N 이 entity 의 `MAX_*_LENGTH` 상수와 동일한가
- [ ] index 가 Exposed `.index()` / `uniqueIndex(...)` 와 1:1 정합한가
- [ ] enum 컬럼의 길이(`varchar(20)` 등) 가 미래 enum 추가 여지를 충분히 가지는가

### Soft delete 컬럼 도입

SoftDeletable entity 도입 시 (`entity.md` 참조) 다음 4가지를 한 PR 에서 묶는다.

```sql
ALTER TABLE {table} ADD COLUMN deleted_at TIMESTAMP NULL;
```

- **nullable 필수** — null 이면 미삭제, timestamp 이면 삭제된 시점. `NOT NULL DEFAULT ...` 형태 금지.
- Exposed table: `val deletedAt = timestamp("deleted_at").nullable()` 한 줄.
- domain entity: 생성자에 `deletedAt: Instant? = null` + `SoftDeletable` implement + 상태 전이 메서드 (`edit` 등) 에 `check(!isDeleted)` 가드. `delete()` 도메인 메서드는 미래 invariant 보호용 enabler — 표준 삭제 흐름은 `repository.delete(id)` (`entity.md` / `usecase-implementation.md`).
- 어댑터: `override val deletedAtColumn = {Table}.deletedAt` + `toEntity()` / `insert()` / `update()` 매핑 (`repository.md`). hard delete 어댑터는 `override val deletedAtColumn = null` 한 줄.
- **인덱스 도입 정책**: 현재는 미도입 — `comments` / `pages` / `spaces` 모두 인덱스 없이 운영. 데이터 누적으로 `WHERE deleted_at IS NULL` 비용이 관측되면 별도 티켓에서 partial index 또는 복합 index (예: `pages_space_id_deleted_at_idx`) 검토.

### forward only

- 한번 머지된 마이그레이션 파일은 **수정·삭제하지 않는다.** 실수했으면 새 마이그레이션으로 보정 (`V{ts}__fix_<...>.sql`).
- 롤백 SQL (Flyway `undo`) 도 현재 미도입. 사고 시 hot-fix 마이그레이션으로 forward only 보정.
- 데이터 백필 (`UPDATE ... WHERE ...`) 은 별도 마이그레이션으로 분리해 DDL 과 섞지 않는다.

### 하나의 마이그레이션 = 하나의 의도

- 한 파일에서 여러 테이블을 새로 만드는 init 은 OK (도메인 단위 초기화).
- 그 외에는 한 파일에 한 의도 (테이블 추가 / 컬럼 추가 / 인덱스 추가 / 백필) — 롤백 작업이 단순해진다.
- 같은 PR 의 같은 결정이라도 **테이블별로 ALTER 를 분리한다** — 예: `V..._add_deleted_at_to_pages.sql` + `V..._add_deleted_at_to_spaces.sql`. 운영 사고 시 한 테이블 단위로 롤백/재시도가 가능하고, 다른 테이블이 같은 정책을 부분 도입하는 경우 (예: Page 만 우선 적용) 도 파일 단위로 추적된다.

## Spring Boot 설정

`app/src/main/resources/application.yml`:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
```

- `baseline-on-migrate: false` — 첫 도입 단계라 baseline 불필요. 운영 DB 이관 발생 시 그 PR 에서 `true` 로 전환 + `baseline-version` 명시.
- `flyway-core` + `flyway-database-postgresql` 의존을 `app/build.gradle.kts` 의 `runtimeOnly` 로 추가 (Spring Boot BOM 이 버전 관리).
- 도메인 `app` 모듈 (`lab-space/app` 등) 은 Flyway 의존을 가지지 않는다 — SQL 만 classpath 에 제공.

## 테스트

Repository 테스트는 **실제 Postgres + Flyway 마이그레이션** 환경에서 돈다.

```kotlin
class ExposedSpaceRepositoryTest :
    DescribeSpec({
        val database = PostgresTestContext.database
        val repository = ExposedSpaceRepository()

        afterEach { PostgresTestContext.truncateAll() }

        describe("ExposedSpaceRepository") {
            it("...") { ... }
        }
    })
```

- `lab-space/app/src/test/.../testsupport/PostgresTestContext.kt` — JVM-shared Testcontainer + Flyway migrate + Exposed `Database`.
- `afterEach { truncateAll() }` — `TRUNCATE ... CASCADE` 로 spec 간 격리. 테이블 목록은 `information_schema` 에서 동적으로 수집하므로 새 테이블 추가 시 헬퍼 갱신이 자동.
- Spring `@SpringBootTest` 는 `TestcontainersConfig` 의 `@ServiceConnection PostgreSQLContainer<*>` 빈으로 datasource 가 자동 wiring 됨. `TestSpaceApplication` / `app` 모듈의 `ApplicationTest` 가 `@Import(TestcontainersConfig::class)` 로 가져온다.

### 회귀 가치

마이그레이션 SQL 의 컬럼·타입·인덱스·제약이 Exposed 정의와 어긋나면 repository 테스트가 즉시 실패한다 — schema/entity drift 가 PR 단계에서 잡힌다. `SchemaUtils.create` 시절엔 Exposed 가 매번 스키마를 만들어 SQL 자체의 회귀를 못 잡았던 문제 해결.

## 새 마이그레이션 추가 절차

1. 변경 의도 정하기 (테이블 추가? 컬럼 추가? 백필?). 하나의 의도 = 한 파일.
2. 현재 시각 기준 14자리 타임스탬프 + description 으로 파일명 결정.
   - 예: `V20260520090000__lab_space_add_tags.sql`
3. `lab-{domain}/app/src/main/resources/db/migration/` 안에 SQL 작성.
4. 같은 PR 에서 Exposed `Table` 객체와 entity (필요 시) 같이 갱신.
5. Repository 테스트가 변경된 스키마를 가정하도록 수정 / 추가.
6. `./gradlew :lab-{domain}:app:test` — Testcontainer 가 마이그레이션을 실제 Postgres 에 적용 → SQL 정합성 검증.
7. 로컬에서 `docker compose up -d` 후 `./gradlew :app:bootRun` 한 번 — 실제 Flyway 적용 확인.

## 자주 빠뜨리는 것

- **머지된 마이그레이션을 수정** — Flyway 가 checksum 으로 감지해 startup 실패. 항상 새 파일로 보정.
- **타임스탬프 충돌** — 같은 분에 두 사람이 마이그레이션을 만들면 같은 버전이 나올 수 있다. PR 직전에 14자리 timestamp 를 다시 찍어 충돌 회피.
- **Exposed 테이블에는 컬럼 추가, 마이그레이션 누락** — repository 테스트는 통과해도 마이그레이션 미적용으로 운영 환경에서 컬럼 없음. PR 체크리스트 (필드 추가) 의 마이그레이션 항목 확인.
- **`crispinlab.kopring.exposed` 컨벤션이 적용된 모든 모듈에 마이그레이션을 분산** — Flyway 가 classpath 전체에서 수집해 한 번에 적용한다. 한 모듈에 두지 않고 여러 모듈에 분산하면 PR 충돌·순서 추적이 어렵다. 각 도메인 모듈 안에서만 둔다.
- **`baseline-on-migrate: true` 로 무심코 켜기** — 기존 테이블이 있는 DB 에 마이그레이션을 baseline 으로 인정한다는 뜻. 신규 dev/test 환경에서는 마이그레이션이 적용되지 않을 수 있다. 운영 이관 PR 외에는 `false` 유지.

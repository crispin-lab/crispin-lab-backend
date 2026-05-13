# 에러 메시지

> **이 문서의 범위**: 사용자 응답으로 나가는 예외 메시지의 형식·보안 가이드.
>
> **검증 위치 (require/check)**: `conventions.md` "검증" 섹션
> **외부 응답 보안 (IDOR 등)**: 본 문서 "정보 노출 방지"

## 기본 원칙

- **존댓말** ("~습니다", "~입니다").
- **한 문장**, **구체적**으로.
- 구조: `[무엇이 잘못되었는지]` (+ 선택: `[해결 방법]`).
- "오류가 발생했습니다" 같은 모호한 문구 금지.

## 예외 매핑

`lab-common` 의 예외를 우선 사용한다.

| 상황 | 예외 | 메시지 패턴 | 예시 |
|------|------|------------|------|
| 엔티티 없음 | `NotFoundException` | "{대상}을(를) 찾을 수 없습니다." | "페이지를 찾을 수 없습니다." |
| 권한 없음 | `NotFoundException` (의도적, 아래 IDOR 참조) | 위와 동일 | "페이지를 찾을 수 없습니다." |
| 상태 충돌 | `ConflictException` | "이미 {상태}입니다." | "이미 발행된 페이지입니다." |
| 중복 | `ConflictException` | "이미 {대상}이(가) 존재합니다." | "이미 등록된 슬러그입니다." |
| 잘못된 입력 | `IllegalArgumentException` (`require` 결과) | "{필드}이(가) {문제}합니다." | "제목을 입력해 주세요." |
| 도메인 규칙 위반 | `DomainException` 하위 (필요 시 신규 정의) | 도메인 용어 그대로 | "비공개 페이지는 댓글을 받을 수 없습니다." |

호출은 한 줄로 — `throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)`. ErrorCode 가 응답 `code` 와 default 메시지를 같이 운반한다. 자세한 시그니처/네이밍은 아래 "ErrorCode 시스템" 참조.

## ErrorCode 시스템

`code` (응답 식별자) 와 `defaultMessage` (사용자 메시지) 를 묶어 도메인별로 enum 으로 항목화한다. hhplus 의 거대 단일 ErrorCode enum + 도메인별 예외 클래스 9 개 같은 패턴을 피한다.

### 시그니처와 위치

- `lab-common/.../exception/ErrorCode.kt` — `interface ErrorCode { val code; val defaultMessage }`. **Spring 의존 금지** (HTTP status 는 ErrorCode 가 아니라 예외 타입이 결정).
- 도메인 enum 은 `lab-{domain}/domain` 의 해당 aggregate 패키지(예: `com.crispinlab.space.domain.page.PageErrorCode`).
- enum 이름이 곧 응답 `code` (`PAGE_NOT_FOUND`). 구현은 `override val code: String get() = name`.

```kotlin
enum class PageErrorCode(
    override val defaultMessage: String
) : ErrorCode {
    PAGE_NOT_FOUND("페이지를 찾을 수 없습니다."),
    PARENT_PAGE_NOT_FOUND("부모 페이지를 찾을 수 없습니다.")
    ;

    override val code: String get() = name
}
```

### 네이밍

- enum 클래스: `<Aggregate>ErrorCode` — `PageErrorCode`, `SpaceErrorCode`.
- 항목명: `<AGGREGATE>_<상태>` SCREAMING_SNAKE. 동사 금지 (`PAGE_NOT_FOUND` ⭕, `PAGE_RETRIEVE_FAILED` ❌).

### Cross-aggregate

같은 user-facing 의미면 같은 code 를 재사용한다. 예: `PageRegisteringUseCase.validate()` 에서 부모 스페이스가 없을 때 `SpaceErrorCode.SPACE_NOT_FOUND` 를 import 해서 throw — Page UseCase 가 Space 의 enum 을 참조해도 같은 모듈(`lab-space`) 안이므로 자연스럽다. 같은 메시지를 두 개의 code (`SPACE_NOT_FOUND` vs `PAGE_FAILED_TO_LOAD_SPACE`) 로 갈라 두지 않는다.

### HTTP status 결정

ErrorCode 는 식별자만 운반한다. status 는 **예외 타입** 이 결정:

| 예외 | status |
|------|--------|
| `NotFoundException` | 404 |
| `ConflictException` | 409 |
| `DomainException` (fallback) | 422 |

이 분리로 `lab-common` 이 Spring 에 의존하지 않는다 — ErrorCode enum 도 같이 Spring-free.

### IllegalArgumentException 의 placeholder code

`require` 결과의 `IllegalArgumentException` 은 도메인 ErrorCode 가 붙지 않는다. 핸들러가 placeholder code `"INVALID_REQUEST"` 로 400 응답, 메시지는 `require` 의 람다 결과 그대로. require 메시지는 도메인 용어로 작성되어 있다고 가정.

### Spring validation 어노테이션

`@Valid` / `jakarta.validation.constraints.*` 를 도입할 때는 어노테이션의 `message` 를 **한국어로 명시**한다. 미명시 시 default 메시지(`@NotBlank` → "must not be blank") 가 그대로 응답으로 흘러 룰(존댓말·한국어)과 충돌한다.

```kotlin
// BAD
data class Body(@field:NotBlank val title: String)

// GOOD
data class Body(@field:NotBlank(message = "제목을 입력해 주세요.") val title: String)
```

`MethodArgumentNotValidException` 핸들러는 첫 필드 오류의 `defaultMessage` 를 그대로 응답에 노출하므로, 어노테이션 message 가 사용자 응답이 된다.

## 예외 발생 위치별 사용

| 위치 | 사용할 것 |
|------|----------|
| 엔티티 `init` (형식·길이) | `require(...) { "..." }` → `IllegalArgumentException` |
| 엔티티 메서드 (상태 불변식) | `check(...) { "..." }` → `IllegalStateException` |
| UseCase `validate`, `toEntity` | `lab-common` 예외 직접 throw |
| controller | 던지지 않는다. UseCase 가 던진 것이 그대로 위로. |

## 정보 노출 방지

### 식별자·경로 숨김

ErrorCode 의 `defaultMessage` 에 식별자를 박지 않는다. 메시지 override 가 필요하더라도 ID 는 빼고 작성한다.

```kotlin
// BAD: ID 노출
throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND, "페이지 ${pageId.value}을(를) 찾을 수 없습니다.")

// GOOD
throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
```

### "없음" 과 "권한 없음" 구분 노출 금지

권한이 없는 리소스의 존재 여부를 응답 차이로 노출하면 IDOR/enumeration 공격에 정보를 흘린다.

```kotlin
// BAD: 다른 사용자의 페이지 존재 여부가 노출됨
val page = pageRepository.findBy(pageId)
    ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
if (page.authorId != currentUserId) {
    throw ForbiddenException(...)
}

// GOOD: 존재 + 권한을 같은 응답으로
val page = pageRepository.findBy(pageId)
    ?.takeIf { it.authorId == currentUserId }
    ?: throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
```

### 인증 실패는 사용자/비밀번호를 구분하지 않는다

```kotlin
// BAD: 사용자 존재 여부 노출 (계정 enumeration)
throw NotFoundException(UserErrorCode.USER_NOT_FOUND)

// GOOD: 인증용 ErrorCode 로 통합 (예: AUTH_INVALID_CREDENTIALS)
throw AuthenticationException(AuthErrorCode.INVALID_CREDENTIALS)
```

### 금지 사항

- 스택 트레이스를 응답에 포함하지 않는다.
- 내부 경로(파일 시스템, S3 키 등)를 노출하지 않는다.
- 시크릿(token, key, password) 은 메시지·로그에 절대 포함하지 않는다.
- DB 에러(unique constraint 메시지 등) 를 그대로 흘리지 않는다 — 도메인 메시지로 감싼다.

## 신규 예외 정의

기존 `NotFoundException` / `ConflictException` 으로 충분하면 새 클래스를 만들지 않는다 — ErrorCode 항목 하나만 추가하면 된다.

```kotlin
// 신규 예외 클래스 불필요
throw NotFoundException(PageErrorCode.PAGE_NOT_FOUND)
```

`DomainException` 의 새 하위 타입이 필요할 때만 추가 (예: `UnauthorizedException` 같은 새 status 분기). 이때도 메시지는 ErrorCode 의 `defaultMessage` 가 default — 호출 측이 메시지를 매번 지정하지 않게.

## 한 줄 요약

- ErrorCode enum 으로 응답 code/메시지를 묶고, `lab-common` 예외 시그니처로 throw. ID·경로·존재 여부는 응답에 흘리지 않는다.

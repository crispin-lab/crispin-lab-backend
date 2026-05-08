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

`NotFoundException.of(entity, identifier)` 팩토리는 **메시지에 식별자를 포함**한다 — 사용자 응답으로 그대로 흘리면 ID 가 노출된다. 사용자 응답용으로는 식별자를 빼고 직접 생성한다 (아래 "정보 노출 방지" 참조).

## 예외 발생 위치별 사용

| 위치 | 사용할 것 |
|------|----------|
| 엔티티 `init` (형식·길이) | `require(...) { "..." }` → `IllegalArgumentException` |
| 엔티티 메서드 (상태 불변식) | `check(...) { "..." }` → `IllegalStateException` |
| UseCase `validate`, `toEntity` | `lab-common` 예외 직접 throw |
| controller | 던지지 않는다. UseCase 가 던진 것이 그대로 위로. |

## 정보 노출 방지

### 식별자·경로 숨김

```kotlin
// BAD: ID 노출
throw NotFoundException("페이지 ${pageId.value}을(를) 찾을 수 없습니다.")

// GOOD: 식별자 없이
throw NotFoundException("페이지를 찾을 수 없습니다.")
```

### "없음" 과 "권한 없음" 구분 노출 금지

권한이 없는 리소스의 존재 여부를 응답 차이로 노출하면 IDOR/enumeration 공격에 정보를 흘린다.

```kotlin
// BAD: 다른 사용자의 페이지 존재 여부가 노출됨
val page = pageRepository.findBy(pageId)
    ?: throw NotFoundException("페이지를 찾을 수 없습니다.")
if (page.authorId != currentUserId) {
    throw ForbiddenException("접근 권한이 없습니다.")
}

// GOOD: 존재 + 권한을 같은 응답으로
val page = pageRepository.findBy(pageId)
    ?.takeIf { it.authorId == currentUserId }
    ?: throw NotFoundException("페이지를 찾을 수 없습니다.")
```

### 인증 실패는 사용자/비밀번호를 구분하지 않는다

```kotlin
// BAD: 사용자 존재 여부 노출 (계정 enumeration)
throw NotFoundException("등록되지 않은 이메일입니다.")

// GOOD
throw DomainException("이메일 또는 비밀번호가 올바르지 않습니다.")
```

### 금지 사항

- 스택 트레이스를 응답에 포함하지 않는다.
- 내부 경로(파일 시스템, S3 키 등)를 노출하지 않는다.
- 시크릿(token, key, password) 은 메시지·로그에 절대 포함하지 않는다.
- DB 에러(unique constraint 메시지 등) 를 그대로 흘리지 않는다 — 도메인 메시지로 감싼다.

## 예외 정의 시 기본 메시지

신규 예외는 기본 메시지를 가지고, 호출 측이 별도 메시지를 줄 일이 거의 없도록 한다.

```kotlin
class PageNotFoundException(
    message: String = "페이지를 찾을 수 없습니다.",
) : DomainException(message)
```

상황별로 메시지를 바꿔야 하면 별도 예외로 분리하는 편이 호출부가 더 깔끔하다.

## 한 줄 요약

- `lab-common` 예외 우선, 도메인 메시지로 감싸고, ID·경로·존재 여부를 응답에 흘리지 않는다.

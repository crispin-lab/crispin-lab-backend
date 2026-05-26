---
name: convention-check
description: PR 전 컨벤션 체크. 프로젝트의 conventions.md / architecture.md 기준으로 변경된 코드를 사전 리뷰한다.
---

`.claude/rules/conventions.md` 와 `.claude/rules/architecture.md` 를 기준으로 현재 변경사항을 사전 리뷰합니다.

## 실행 순서

### 1단계: 컨벤션 문서 로드

- `.claude/rules/conventions.md` 를 읽는다.
- `.claude/rules/architecture.md` 를 읽는다 (모듈 경계·계층 책임 검증용).
- `.claude/rules/project-context.md` 를 읽는다 (도메인 용어 검증용).

### 2단계: 변경 파일 파악

- `git diff --name-only HEAD` 로 변경된 파일 목록을 확인한다.
- 인자(`$ARGUMENTS`)가 있으면 해당 파일만 대상으로 리뷰한다.
- 변경이 없으면 staged 변경(`git diff --name-only --cached`)을 본다.

### 3단계: 변경 내용 분석

변경된 파일들의 diff 를 읽고 작업 유형을 판별한다:

- 필드 추가
- 새 API / 새 컨트롤러
- 버그 수정
- 외부 연동
- 조회 쿼리
- 리팩토링 / 모듈 경계 이동

판별한 유형에 맞는 체크리스트(conventions.md "PR 전 사전 리뷰 체크리스트" 섹션)를 적용한다.

### 4단계: 리뷰 보고

다음 항목을 conventions.md / architecture.md 기준으로 점검한다.

**공통 체크 (conventions.md):**

- **네이밍** — 도메인 의미 우선, suffix 남용 금지, 시점 필드 의미, `With` 전치사 규칙, 상태 변경 메서드는 현재동사
- **타입 안정성** — `Any` 회피, 도메인 영역 String 회피, 타입 일관성, `data class` 불변성
- **검증** — `require` / `check` 구분, `require { throw }` 금지
- **함수형 스타일** — `let` 활용, `filter { != }` 선호, `takeIf`, 빈 컬렉션 분기 불필요
- **확장 함수** — non-nullable 수신자 선호, 객체지향적 호출 스타일
- **Static Import** — 내부 클래스, enum companion, Java static 메서드
- **상수화** — 한 곳에서만 쓰이는 리터럴 상수화 금지
- **테스트** — 실제 회귀를 잡는가, mock/stub 사용, 컨텍스트 중복 금지, fixture 기본값 주석 금지
- **포맷팅** — 100자 라인, trailing comma 비허용, 와일드카드 import 금지
- **에러 메시지** — 구체적 한 문장, 내부 정보 노출 금지

**모듈 경계 체크 (architecture.md):**

- `lab-space/domain` 에 Spring / Exposed / HTTP import 가 있는가 → **즉시 차단**
- `lab-common` 에 도메인 로직이 들어갔는가
- `app` 모듈만 `bootJar` 활성화, 나머지는 라이브러리 모드인가
- `io.spring.dependency-management` 플러그인을 새로 추가했는가 → **금지**

**도메인 용어 체크 (project-context.md):**

- `Space`, `Page`, `PageRevision`, `PageLink`, `Comment`, `Tag` 등 정해진 용어를 일관되게 쓰는가
- 현재 스코프 외(ES 검색 구현)에 손대고 있지 않은가

**작업 유형별 체크 (conventions.md "PR 전 사전 리뷰 체크리스트" 섹션):**

판별한 작업 유형의 체크리스트를 항목별로 검증한다.

### 5단계: 보고 형식

```
⚠️⚠️ 필수 수정
- [path/to/File.kt:42] 어떤 위반인지 — (해당 컨벤션 섹션명)
  근거: 짧은 인용 또는 설명

⚠️ 권장 개선
- [path/to/File.kt:88] 개선 제안 — (해당 섹션명)

✅ 잘 지킨 항목
- 항목 (간단히)
```

- 위반 위치는 반드시 `파일:라인` 으로 표시한다.
- 해당 컨벤션 섹션명을 함께 적는다 (예: "네이밍 — 날짜 필드", "검증 — `require` vs `check`").
- 모듈 경계 위반은 **필수 수정** 으로 분류한다.
- 우선순위가 모호하면 "필수 수정" 으로 안전하게 분류한다.

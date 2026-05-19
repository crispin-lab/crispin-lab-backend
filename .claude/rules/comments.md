# 주석 / KDoc

> **이 문서의 범위**: 주석·KDoc 의 작성 정책. *무엇을 쓰지 않을지* + *어디로 옮길지*.
>
> **문서 언어 / 톤**: `docs-style.md`

## 핵심 원칙

코드로 의도가 드러나게 작성한다. 주석을 추가하기 전에 **이름·타입·구조 변경으로 해결 가능한지** 먼저 본다. KDoc (`/** ... */`) 도 같은 기준 — "주석은 안 쓰지만 KDoc 은 OK" 가 아니다.

같은 패턴이나 *왜* 가 두 번째 등장하거나, 한 곳에 적기엔 분량이 크면 **코드 주석이 아니라 `.claude/rules/<topic>.md` 룰 문서** 로 옮긴다. 룰 문서는 검색·grep 이 가능하고, 코드 리네임·이동에서 떨어져 나가지 않고, PR 리뷰에 자연스럽게 들어온다.

## 쓰지 않는 것

| 패턴 | 이유 | 대신 |
|------|------|------|
| 메서드 헤더 KDoc 으로 "무엇을 하는지" 서술 | 함수명·시그니처가 한다 | 함수 이름·파라미터 이름을 다듬는다 |
| 변수 옆 한 줄 주석으로 의미 부연 | 변수명이 한다 | 변수명을 바꾼다 |
| 코드 블록 prologue ("이 부분은 X 를 한다") | 함수 분리로 표현 | private 함수로 추출하고 이름으로 설명 |
| public 메서드 모든 곳에 KDoc 의무화 | 노이즈, 정보 가치 낮음 | 시그니처로 충분하면 KDoc 없음 |
| 매직 넘버 옆 부연 주석 (`// HTTP OK`) | 상수가 한다 | named 상수로 추출 (`conventions.md` "상수화") |
| 삭제·이동 흔적 (`// 이전엔 X 였음`, `// removed for now`) | git history / PR 본문이 한다 | 삭제 |
| 흐릿한 TODO (`// TODO: clean up later`) | 의도 불명, 부패 빠름 | 진짜 TODO 면 아래 블록 템플릿, 아니면 삭제 |

## 정말 필요한 경우만

다음 셋 중 하나에 명확히 해당하면 *한 줄* 주석을 둔다.

- **숨겨진 invariant** — 코드만 봐서는 모를 사실 (예: "다른 SQL 에서만 채워지는 컬럼")
- **워크어라운드** — 라이브러리 버그·플랫폼 quirks. 출처(이슈 URL, 버전) 포함
- **비직관적 알고리즘의 *왜*** — 성능, 일관성, race 회피 같은 트레이드오프

세 가지 모두에 해당하지 않으면 **삭제**. 코드 리뷰에서 "이 주석 있어야 하나?" 의 답이 "굳이는 아니다" 면 지운다.

## 룰 문서로 옮기는 신호

다음 중 하나가 보이면 코드 주석이 아니라 룰 문서로.

- **같은 "왜" 가 두 번째 등장** — 한 줄을 두 번 복붙하느니 룰 문서에 한 절 추가.
- **분량 3 줄 초과** — 코드 위에서 흐름이 끊긴다. 룰 문서가 더 잘 담는다.
- **다른 곳의 invariant 와 연결** — "이 어댑터의 `keys` 가 바뀌면 X 가 깨진다" 류는 해당 룰 (`repository.md` 등) 의 정해진 절에 한 줄 추가가 정합.
- **PR 리뷰에서 두 번째 사람이 같은 질문** — 코드 옆이 아니라 룰 문서에서 답하게.

코드 쪽에 남기는 마지막 흔적은 **이름** — 상수 (`UNIQUE_VIOLATION_SQL_STATE`), 함수명 (`validate`, `toEntity`, `editWith`) — 한 단어로 충분히 검색·추적되게 한다.

## Before / After

### 메서드 헤더 KDoc 제거

```kotlin
// BAD
/**
 * base 의 upsert 가 PK 충돌은 `ON CONFLICT (id)` 로 흡수하므로 잔여 SQLState 23505 는
 * `tags_space_id_name_uidx` 위반뿐 — UseCase 의 `existsByNameAndSpaceId` 사전 체크가
 * 통과한 뒤 race window 에서 동시 INSERT 가 들어왔을 때만 발생한다.
 * `keys` 시그니처가 도입되면 본 가정이 깨지므로 같이 갱신.
 */
override fun save(entity: Tag): Tag = ...

// GOOD — 같은 사실을 `repository.md` 의 "race 안전 (PK 한정)" 절에 한 줄로 두고, 코드는 비운다.
override fun save(entity: Tag): Tag = ...
```

### 변수 부연 → 변수명 변경

```kotlin
// BAD
val s = "23505"     // Postgres unique_violation SQLState

// GOOD
const val UNIQUE_VIOLATION_SQL_STATE = "23505"
```

### 블록 prologue → 함수 추출

```kotlin
// BAD
fun perform(request: Request): Result {
    // 권한 확인 + 페이지 조회
    val page = pageRepository.findBy(request.pageId)
        ?.takeIf { it.authorId == request.currentUserId }
        ?: throw NotFoundException(PAGE_NOT_FOUND)
    ...
}

// GOOD
fun perform(request: Request): Result = request
    .also { it.validate() }
    .toEntity()
    ...

private fun Request.toEntity(): Page =
    pageRepository.findBy(pageId)
        ?.takeIf { it.authorId == currentUserId }
        ?: throw NotFoundException(PAGE_NOT_FOUND)
```

## TODO 주석 — 유일한 템플릿 예외

진짜로 미루는 작업은 다음 블록 한 가지 형식으로만 쓴다.

```kotlin
/*
todo    :: 앞으로 해야 할 작업 설명 (왜 이 주석을 남겼고 어떤 작업을 할 것인지)
 author :: <사람 이름>
 date   :: 2026-05-11T14:30:39KST
 ticket :: LAB-31
 */
```

- `todo`, `author` 필수. `date`, `ticket` 도 가능하면 채운다 (`ticket` 은 Jira 티켓이 있을 때만).
- 라벨 (`todo` / `author` / `date` / `ticket`) 영역을 8칸 너비로 패딩해 라벨 뒤 `::` 위치를 정렬 — 첫 줄(`todo`) 만 앞 공백 없음, 나머지는 앞 공백 1.
- IntelliJ 기본 TODO 패턴 (`\btodo\b`, case-insensitive) 이 `todo    ::` 를 인식해 TODO 패널에 자동 노출.
- `todo` 본문은 한 줄로 간결하게 + **단일 판단 기준** ("X 가 생기면 유지, 아니면 제거" 같은 양갈래 분기 지양). 100 자 라인 룰 그대로.
- 작업이 완료되면 TODO 주석 자체를 제거한다 (stale TODO 가 남지 않게).
- `author` 는 git config 의 username 이 아니라 사람 이름.
- `date` 는 `YYYY-MM-DDTHH:mm:ssKST` 형식 (주석을 남긴 시점). ISO 8601 의 타임존은 `+09:00`/`Z` 가 표준이지만 KST 가독성을 우선해 비표준 `KST` 표기 사용.

본 템플릿은 **TODO 전용**. 일반 "왜" 주석은 평문 한 줄로.

## 자주 빠뜨리는 것

- **"코드 보면 알지" 류 KDoc** — `/** edit 메서드. 페이지를 수정한다. */`. 시그니처가 같은 말을 한다. 삭제.
- **author / @date 같은 템플릿 주석** — git 이 관리. (TODO 블록 안의 `author`/`date` 는 작업 의도 메타데이터라 다름)
- **삭제한 코드 옆 흔적** — `// 이전엔 X 였음`. PR 본문·git blame 이 한다.
- **같은 "왜" 주석을 두 곳에 복붙** — 두 번째 등장이 신호. 룰 문서로.
- **인터페이스 method 마다 KDoc** — 시그니처와 이름이 한다. 진짜 비직관적 invariant 만 한 줄.
- **`@author`, `@since`, `@param`, `@return` 같은 Javadoc 류 태그** — Kotlin 에서는 시그니처가 표현. 사용 금지.

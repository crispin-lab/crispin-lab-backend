# 코딩 컨벤션

> **이 문서의 범위**: 네이밍, 타입, 검증, 함수 작성, 테스트, 포맷팅 등 **코딩 스타일과 컨벤션**.
>
> **모듈 레이아웃·계층 경계**: `architecture.md` 참조
> **도메인 용어**: `project-context.md` 참조

## 기본 원칙

- 모든 규칙의 판단 기준: **유지보수에 도움이 되는가?**
- IDE 경고는 무시하지 않는다. 무시해야 하면 `@Suppress`로 명시한다.
- 한 곳에서만 쓰이는 헬퍼/상수는 만들지 않는다. 인라인이 낫다.
- 성급한 공통화 지양. 중복이 실제 통증이 될 때 추출한다.

## 네이밍

### 도메인 의미를 먼저 드러낸다

이름만 보고 역할이 추론되어야 한다. 구현 디테일(`Queue`, `Http`, `Slack`, `Dto`)은 앞에 노출하지 않는다.

```kotlin
// BAD
class PageRequest                       // 너무 넓음 — 무엇에 대한 요청?
fun sendUpdatedPageMessage()            // "Message" 가 구현 디테일
data class PageDto(...)                 // Dto suffix

// GOOD
class PageEditingRequest
fun notifyPageUpdate()
data class PageSummary(...)             // 역할 노출 (Summary, Snapshot, Payload)
```

- 컨트롤러, 유스케이스 인터페이스/구현체, 포트는 같은 유비쿼터스 언어를 쓴다.
- `Dto`, `~Data`, `~Info`, `~Response`, `~Entity` 같은 의미 없는 suffix 는 피한다.
- 미국식 영어를 기본으로 (예: `CANCELED`).

### 값 획득 메서드는 명사형

```kotlin
// GOOD
fun totalCount(): Int                   // 값 획득 → 명사
val status: Status

// BAD
fun calculateTotalCount(): Int          // calculate 는 동작
fun getStatus(): Status                 // getter 스타일
```

### 상태 변경 메서드는 현재동사

```kotlin
// GOOD
fun publish() { status = PUBLISHED }
fun archive() { status = ARCHIVED }

// BAD
fun published() {}                      // 과거형 — 이미 완료된 것처럼 읽힘
fun setArchived() {}                    // setter 스타일
```

### 날짜 필드는 시점 의미를 정확히

`~edAt` 은 **이미 발생한 시점**에만 사용한다. 미래/예정 시점은 `expected~` 또는 다른 형태로.

```kotlin
// GOOD
createdAt           // 생성된 시점 (이미 발생)
publishedAt         // 발행된 시점
deletedAt           // 삭제된 시점 (soft delete)
expectedReviewAt    // 검토 희망 시점 (아직 발생하지 않음)

// BAD
publishAt           // 현재형 — 의미 모호
inboundedAt         // inbound 는 형용사이므로 과거분사 불가
```

`updatedAt` 은 "어떤 변경이든 발생한 시점"이다. 특정 비즈니스 시점(`publishedAt` 등)을 대체한다고 가정하지 않는다.

### `With` 전치사 규칙

별도 Request DTO 를 받아 동작하는 메서드에는 `With` 를 붙이고, 도메인 엔티티를 직접 받는 메서드에는 붙이지 않는다. `With` 앞은 **명사**.

```kotlin
// GOOD
fun publishWith(request: PublishRequest)
fun notifyRegistrationWith(request: RegistrationRequest)
fun publish(page: Page)

// BAD
fun notifyRegisteredWith(request: ...)  // 과거분사 — 어색함
```

### nullable 반환과 메서드명

시그니처가 이미 `?` 로 말해주므로 `OrNull` 접미사를 붙이지 않는다.

```kotlin
// GOOD
fun findBy(id: PageId): Page?

// BAD
fun findByOrNull(id: PageId): Page?
```

### 하위 클래스 타입명 축약

부모 컨텍스트가 반복되면 줄인다.

```kotlin
// BAD
data class PageLink(val type: PageLinkType) {
    enum class PageLinkType { INTERNAL, EXTERNAL }
}

// GOOD
data class PageLink(val type: Type) {
    enum class Type { INTERNAL, EXTERNAL }
}
```

## 타입 안정성

### `Any` 사용 지양

`Any` 대신 구체 타입, 제네릭 `<T>`, `sealed interface`, `Map<String, Any?>` 사용.
예외: JSON 라이브러리 내부, 프레임워크 유틸, Reflection.

### core 에서는 String 보다 도메인 타입을 우선

URL, enum, phone, email, 정렬이 의미 있는 컬렉션은 `String`/`Collection` 으로 넓게 두지 않는다.

```kotlin
// BAD
data class PageEditingRequest(
    val visibility: String,
    val coverImageUrl: String,
)

// GOOD
data class PageEditingRequest(
    val visibility: Visibility,
    val coverImageUrl: URL,
)
```

- API 경계(controller / infra)는 외부 스펙에 맞추되, 도메인 내부는 구체 타입을 쓴다.
- 순서가 의미 있으면 `Collection` 대신 `List`.
- 허용값이 정해진 입력은 enum 또는 값 객체.

### 타입 일관성 유지

같은 데이터의 변환(`toString()`, `toLong()`)이 여러 곳에 흩어져 있으면 설계 문제다.
변환은 경계(Controller/Adapter)에서 한 번만, 도메인 내부는 일관 타입 사용.

### 타입 추론이 가능하면 타입 생략

```kotlin
// BAD
val page: Page = pageRepository.findBy(id)

// GOOD
val page = pageRepository.findBy(id)
```

### `data class` 는 불변 유지

```kotlin
// BAD
data class PageSummary(
    val id: PageId,
    var viewCount: Int = 0,             // 변경 가능 → 혼란
)

// GOOD: 일반 class 로 바꾸거나 copy() 로 새 인스턴스
data class PageSummary(
    val id: PageId,
    val viewCount: Int,
)
```

## 검증

### `require` vs `check`

- **외부 입력 검증**: `require` (생성자, Request 변환, public 메서드 진입부)
- **내부 상태 불변식**: `check` (엔티티 메서드 안에서 상태 기반 제약)

```kotlin
// 외부 입력
require(title.isNotBlank()) { "제목을 입력해 주세요." }

// 내부 상태
check(status == DRAFT) { "이미 발행된 페이지는 발행할 수 없습니다." }
```

### `require { throw }` 금지

`require` 의 람다는 메시지 String 반환용이다. `throw`를 넣지 않는다.

```kotlin
// BAD
require(ids.isNotEmpty()) { throw IllegalArgumentException("...") }

// GOOD
if (ids.isEmpty()) throw IllegalArgumentException("페이지를 선택해 주세요.")
```

### 외부 입력은 일찍 검증한다

설정값, 요청값, DB 정책값을 "일단 믿고" 흘려보내지 않는다. 무한 시퀀스, 무제한 재시도, 과도한 payload 읽기는 상한을 둔다.

### 검증 책임 분리

| 위치 | 대상 | 예시 |
|------|------|------|
| 엔티티 `init` | 필드 형식·길이·빈 값 | `require(title.isNotBlank())` |
| 엔티티 메서드 | 상태 기반 제약 | `check(status == DRAFT)` |
| 유스케이스 | 외부 의존성이 필요한 검증 | 권한, 엔티티 존재 여부 |

## 함수형 스타일

### 기본 원칙

가독성 우선. 함수형을 선호하되, 가독성이 떨어지면 명령형이 낫다.

### 복잡도 관리

지역 변수보다 함수 체이닝 선호. 체이닝이 깊어지면 **확장 함수로 분리**.

```kotlin
// GOOD
fun calculateScore(): Int =
    page.revisions.recent().sumOf { it.weight() }

private fun List<Revision>.recent() = filter { it.isRecent() }
private fun Revision.weight() = if (major) 10 else 1
```

### `let` 으로 블록 분리

성격이 다른 단계(구성 → 실행)를 분리할 때 지역변수 대신 `let`.

```kotlin
// GOOD
buildPayload(page)
    .let { payload ->
        runCatching { send(payload) }
    }

// BAD
val payload = buildPayload(page)
runCatching { send(payload) }
```

### `let` 블록 안에서 `return` 지양

체인 결과가 그대로 반환되도록 구성.

```kotlin
// BAD
private fun Page.toResult(): Result =
    findBy(id).let {
        return Result(...)              // let 블록 안에서 return
    }

// GOOD
private fun Page.toResult(): Result =
    findBy(id).let { fetched ->
        Result(...)                     // 체인 결과가 그대로 반환
    }
```

### nullable 처리

early return (`?: return`) 또는 `let` 체이닝 선호. `if (x != null)` 중첩 지양.

### `filterNot` 보다 `filter { != }`

조건을 반대로 읽지 않게 한다.

```kotlin
// BAD
items.filterNot { it == EXCLUDED }

// GOOD
items.filter { it != EXCLUDED }
```

### `takeIf` 활용

```kotlin
// BAD
val page = repository.findBy(id)
if (page?.spaceId != spaceId) throw NotFoundException(...)

// GOOD
val page = repository.findBy(id)
    ?.takeIf { it.spaceId == spaceId }
    ?: throw NotFoundException(...)
```

### 빈 컬렉션 분기 불필요

빈 리스트에 `map`, `filter` 등을 호출해도 안전하다.

```kotlin
// BAD
if (items.isNotEmpty()) items.map { it.toResult() } else emptyList()

// GOOD
items.map { it.toResult() }
```

## 확장 함수

코드를 서술적으로 만들기 위해 적극 활용. 단, 네이밍에 각별히 주의.

```kotlin
// GOOD
page.calculateScore()
revisions.filterRecent()

// BAD
page.calc()
revisions.filter2()
```

### 호출 스타일

객체의 메서드처럼 호출 (객체지향 스타일 유지).

```kotlin
// GOOD
result.toEntity()
payload.validateKeys()

// BAD
toEntity(result)
validateKeys(payload)
```

### 수신자는 non-nullable 선호

호출부에서 `?.` 을 쓰는 편이 낫다.

```kotlin
// BAD
fun Page?.score(): Int? = this?.revisions?.sumOf { it.weight }

// GOOD
fun Page.score(): Int = revisions.sumOf { it.weight }
// 호출: page?.score() ?: 0
```

## Static Import

물리적 코드양을 줄이고 흐름을 따라가기 쉽게 만든다.

### 내부 클래스(Nested Class) 직접 참조

```kotlin
// GOOD
import com.crispinlab.space.application.port.incoming.PageEditing
import com.crispinlab.space.application.port.incoming.PageEditing.Request
import com.crispinlab.space.application.port.incoming.PageEditing.Result

class PageEditingUseCase : PageEditing {
    override fun perform(request: Request): Result { ... }
}

// BAD: 부모 클래스를 통한 접근
override fun perform(request: PageEditing.Request): PageEditing.Result
```

### Java static 메서드도 static import

```kotlin
// BAD
val occurredAt: Instant = Instant.now()

// GOOD
import java.time.Instant.now
val occurredAt = now()
```

### enum companion object 안에서는 enum 접두어 생략

```kotlin
enum class Visibility {
    PUBLIC, INTERNAL, PRIVATE;

    companion object {
        // GOOD
        val openValues = entries.filter { it != PRIVATE }

        // BAD
        val openValues = Visibility.entries.filter { it != Visibility.PRIVATE }
    }
}
```

### `this` 키워드 생략

이름 충돌이 없으면 `this` 를 생략한다.

## 상수화

### 상수화가 필요한 경우

- 여러 곳에서 같은 값 재사용
- 숫자의 의미가 코드만으로 불명확
- 변경 가능성이 있는 비즈니스 정책 값

### 상수화가 불필요한 경우

- 한 곳에서만 사용
- 파라미터 이름으로 의미가 명확
- 표준 관례 (예: HTTP 200..299)

```kotlin
// BAD: 한 곳에서만 사용 → 상수화 이점 없음
companion object {
    private const val SLACK_URL = "https://slack.com/api/chat.postMessage"
}
fun send() = httpRequest(url = SLACK_URL)

// GOOD: 인라인
fun send() = httpRequest(url = "https://slack.com/api/chat.postMessage")
```

### Companion Object 로 추출

함수 호출마다 반복 생성되는 불변 컬렉션은 companion object 로 추출.

```kotlin
// BAD
private fun validate() {
    val sensitive = listOf("password", "token", "secret")
    sensitive.forEach { ... }
}

// GOOD
companion object {
    private val SENSITIVE_KEYWORDS = listOf("password", "token", "secret")
}
```

## 우발적 중복

```kotlin
// 중복으로 보여도 무조건 공통화하지 않는다.
// 사용처 변경 가능성이 낮을 때만 공통화.
// 공통 코드에 분기를 추가하는 것보다는 코드 분리를 우선한다.
```

### 단일 사용 헬퍼 메서드 제거

헬퍼는 **2번 이상 사용**될 때만 만든다. 한 곳에서만 쓰이면 인라인.
예외: 복잡한 알고리즘 분리, 테스트가 필요한 독립적 로직.

## 주석

- 코드로 의도가 드러나게 작성. 정말 필요한 경우만 주석.
- *왜* 그렇게 작성했는지 (의도, 제약, 트레이드오프)만 적는다. *무엇* 을 하는지는 코드가 말한다.
- author 주석 등 템플릿 주석 비활성화 (git 이 관리). **단, TODO 주석은 아래 블록 템플릿을 따른다.**

### TODO 주석 표기

새 TODO 주석은 다음 블록 형식으로 작성한다. 라벨 (`todo` / `author` / `date` / `ticket`) 영역을 8칸 너비로 패딩해 라벨 뒤 `::` 위치를 정렬한다 — 첫 줄(`todo`)만 앞 공백 없음, 나머지는 앞 공백 1.

```kotlin
/*
todo    :: 앞으로 해야 할 작업 설명 (왜 이 주석을 남겼고 어떤 작업을 할 것인지)
 author :: <사람 이름>
 date   :: 2026-05-11T14:30:39KST
 ticket :: LAB-31
 */
```

- `todo`, `author` 는 필수. `date`, `ticket` 도 가능하면 채운다 (`ticket` 은 Jira 티켓이 있을 때만).
- IntelliJ 기본 TODO 패턴 (`\btodo\b`, case-insensitive) 이 `todo    ::` 를 인식해 TODO 패널에 자동 노출한다.
- 위 "주석" 섹션의 일반 룰(코드로 의도 드러내기, author 표기 금지) 은 **TODO 가 아닌 일반 "왜" 주석** 에 그대로 적용 — 본 블록 템플릿은 TODO 전용.
- `todo` 본문은 한 줄로 간결하게 + **단일 판단 기준** ("X 가 생기면 유지, 아니면 제거" 같은 양갈래 분기 지양). 100 자 라인 룰을 그대로 따른다.
- 작업이 완료되면 TODO 주석 자체를 제거한다 (stale TODO 가 코드에 남지 않게).
- `author` 는 git config 의 username 이 아니라 사람 이름.
- `date` 는 `YYYY-MM-DDTHH:mm:ssKST` 형식 (주석을 남긴 시점). ISO 8601 의 타임존은 `+09:00`/`Z` 가 표준이지만 KST 가독성을 우선해 비표준 `KST` 표기 사용.

## 함수 호출 가독성

### 파라미터 이름 명시

파라미터가 하나여도 이름을 명시한다.

```kotlin
PageRequest(page = 0, size = 20)
firstPage(size = 50)
```

### 인자가 2개 이상이면 호출도 개행

named 인자가 2개 이상이면 — 한 줄에 들어간다 해도 — 한 인자/한 줄 + 닫는 괄호 새 줄로 개행한다. 인자 이름이 시각적으로 정렬돼 누락·순서 오류가 잡히고, diff 가 줄 단위로 떨어진다.

```kotlin
// GOOD
Request(
    pageId = pageId,
    currentUserId = auth.userId
)

Body.toRequestWith(
    pageId = pageId,
    userId = auth.userId
)

// BAD
Request(pageId = pageId, currentUserId = auth.userId)
```

인자가 하나인 단순 호출(`PageId(idGenerator.next())`, `useCase.perform(it)`) 은 한 줄 유지.

### 람다 블록 시작/끝은 반드시 개행

블록 경계를 명시적으로 드러내기 위함. `check`, `require` 메시지 람다도 예외 없이 적용.

```kotlin
// GOOD
request
    .also {
        it.validate()
    }.toResult()

require(title.isNotBlank()) {
    "제목을 입력해 주세요."
}

// BAD
request.also { it.validate() }.toResult()
require(title.isNotBlank()) { "제목을 입력해 주세요." }
```

`map`/`filter`/`flatMap` 처럼 컬렉션을 **변환만** 하는 람다는 한 줄로 둔다 (`items.map { it.toResult() }`) — 변환은 흐름의 한 단계가 아니라 값의 모양 그 자체라, 개행하면 오히려 시선이 끊긴다. 같은 식 안에서 변환과 부수효과(`also`/`let` 등) 가 섞이면 부수효과 람다부터 개행한다.

### 블록 변수 vs `it`

짧은 블록은 `it`, 길거나 중첩되면 명시적 변수.

```kotlin
// GOOD
items.map { it.toResult() }

summary.let { pageSummary ->
    Result(
        id = pageSummary.id,
        title = pageSummary.title,
        ...
    )
}
```

## 테스트

### 프레임워크

- **Kotest DescribeSpec** + Mockk
- 컨벤션 플러그인 `crispinlab.kotest` 가 자동 적용

### 최소 커버리지

- 성공 케이스 + 주요 예외 케이스
- 새로 추가된 분기, 저장 로직, null 처리, 실패 분기마다 검증 케이스 추가
- 실제 회귀를 잡는 테스트를 우선한다 (메서드 직접 호출 수준에 그치지 않는다)

### 외부 의존성은 mock/stub

실제 네트워크/DB 의존을 테스트에 끌어들이지 않는다. flaky 의 원인이 된다.

### 컨텍스트 중복 금지

`describe` 등 상위 컨텍스트가 도메인을 이미 설명하면, 하위 설명에서 같은 접두어를 반복하지 않는다.

```kotlin
// GOOD
describe("페이지 발행") {
    it("제목이 비어 있으면 실패한다") { ... }
}

// BAD
describe("페이지 발행") {
    it("페이지 발행 시 제목이 비어 있으면 실패한다") { ... }
}
```

### Fixture 기본값에 설명 주석 금지

기본값이 테스트 의도에 맞으면 그대로 쓴다. 테스트 이름과 `.copy()` 대비로 충분.

```kotlin
// GOOD
it("정상적으로 발행한다") {
    val request = basicRequest()
}

it("제목이 비어 있으면 실패한다") {
    val request = basicRequest().copy(title = "")
}

// BAD
it("정상적으로 발행한다") {
    val request = basicRequest()    // title = "테스트", visibility = PUBLIC
}
```

### Fixture 함수는 static import

```kotlin
// GOOD
import com.crispinlab.space.testsupport.Fixtures.basicPage
val page = basicPage()

// BAD
val page = Fixtures.basicPage()
```

### 테스트에서도 프로덕션 상수 재사용

문자열 리터럴보다 프로덕션 상수를 쓰면 변경에 따라 테스트도 같이 움직인다.

## 포맷팅 (.editorconfig)

- 최대 라인 길이: 100자
- 들여쓰기: 4 spaces
- Continuation 들여쓰기: 8 spaces
- Trailing comma 비허용 (`ij_kotlin_allow_trailing_comma = false`)
- 다중 라인 호출 파라미터는 한 줄에 하나씩, 닫는 괄호는 새 줄
- **와일드카드 import 금지**
- KtLint(Kotlinter) 자동 포맷팅 적용 — git pre-commit hook 으로 강제

## 의존성 관리

- 버전은 `gradle/libs.versions.toml` 에서 관리한다 (version catalog).
- Spring Boot BOM(`crispinlab.kopring.base` 안에서 `platform()`)이 관리하는 의존성은 **버전 명시 금지**.
- BOM 에 없는 의존성만 catalog 에 명시 버전을 둔다.
- `io.spring.dependency-management` 플러그인은 사용하지 않는다.

## 에러 메시지 / 보안

세부 패턴은 `error-messages.md` 참조. 한 줄 요약:

- 존댓말, 한 문장, 구체적. `lab-common` 예외(`DomainException`, `NotFoundException`, `ConflictException`) 우선.
- 메시지에 내부 ID·경로·시크릿·스택 트레이스 노출 금지.
- "없음" 과 "권한 없음" 을 응답으로 구분하지 않는다 (IDOR/enumeration 방지).

## 코드 위생

### 미사용 코드 즉시 제거

방향 전환·리팩토링 후 남은 미사용 코드는 그 자리에서 지운다.

특히 **구현 없는 포트(인터페이스만 있고 구현체 없음)** 는 빌드는 통과하지만 런타임 DI 실패로 배포가 깨진다. 구현 준비가 안 됐으면 포트 자체를 머지하지 않는다.

### 패키지 이동 후 import 검증

패키지를 옮기면 IDE 가 import 를 자동 수정하지만, 간혹 깨진다:

- `_root_ide_package_` 가 import 에 남는 경우
- static import 가 풀려 FQCN 이 코드에 직접 노출되는 경우

→ 패키지 이동 후 반드시 `./gradlew lintKotlin` (또는 `formatKotlin`)과 빌드를 돌린다.

### Exposed 테이블 객체명과 테이블명 일치

```kotlin
// BAD
object PageTable : Table("pages")               // 객체명·테이블명 불일치

// GOOD
object Pages : Table("pages")
```

### S3/외부 저장소 경로는 URL 타입으로

경로를 `String` 으로 다루면 데이터만 보고 위치 추적이 어렵다. 전체 URL 을 `URL` 타입으로 저장.

```kotlin
// BAD
val coverImagePath: String              // "uploads/cover/abc.jpg" — 어디 버킷?

// GOOD
val coverImageUrl: URL
```

### `@JsonProperty` 불필요 시 제거

네이밍 충돌이나 특별한 매핑 이유가 없으면 사용하지 않는다.

## PR 전 사전 리뷰 체크리스트

### 공통

- [ ] 이름이 도메인 용어와 맞는가 (controller, usecase, port, entity 가 같은 용어 체계인가)
- [ ] `With` 전치사가 필요한 메서드에 빠짐없이 붙었는가, `With` 앞이 명사인가
- [ ] `String`, `Any`, 넓은 nullable, 넓은 컬렉션 타입을 불필요하게 쓰지 않았는가
- [ ] 외부 입력 검증과 내부 상태 검증을 `require` / `check` 로 구분했는가
- [ ] 외부 호출, 쿼리 비용, 반복 계산, 무한 탐색에 상한이 있는가
- [ ] 에러 메시지가 구체적이고 내부 정보 노출이 없는가
- [ ] 테스트가 실제 회귀를 잡는가 (메서드 호출만 검증하지 않는가)
- [ ] 한 곳에서만 쓰이는 리터럴을 불필요하게 상수화하지 않았는가
- [ ] 모듈 경계(`architecture.md`)를 위반하지 않았는가 — 특히 `lab-space/domain` 에 Spring/Exposed/HTTP import 가 들어가지 않았는가

### 필드 추가 PR

- [ ] domain entity 필드 추가
- [ ] 수정 가능 필드면 entity 의 update 메서드 반영
- [ ] inbound port (UseCase) Request/Result 반영
- [ ] outbound port (Repository/Search) 반영
- [ ] Exposed table 컬럼 추가, 매핑 반영
- [ ] API request body / response 반영
- [ ] DB migration 추가
- [ ] fixture, usecase 테스트, controller 테스트 반영

### 새 API / 새 컨트롤러 PR

- [ ] 기존 controller 패턴과 같은 패키지 구조를 따르는가
- [ ] request DTO 에서 primitive 를 도메인 타입으로 올렸는가
- [ ] `consumes`, `produces`, path, version 이 실제 계약과 맞는가
- [ ] controller 테스트가 실제 라우팅을 검증하는가

### 버그 수정 PR

- [ ] 버그 원인을 코드상 어디였는지 설명할 수 있는가
- [ ] 수정이 최소 범위로 들어갔는가
- [ ] 재현 테스트가 추가되었는가
- [ ] 근본 원인 대신 증상만 막은 것은 아닌가

### 외부 연동 PR

- [ ] 사용자 요청 경로에서 동기 blocking 호출을 하지 않는가
- [ ] timeout, retry, fallback, logging 전략이 있는가
- [ ] best-effort 인지 필수 처리인지 구분이 명확한가
- [ ] `runCatching { ... }.getOrNull()` 로 실패를 조용히 삼키고 있지 않은가

## 한 줄 요약

- 이름은 도메인 의미 중심으로
- 도메인 영역은 구체 타입 중심으로
- 입력/설정은 초기에 검증
- 테스트는 실제 회귀를 잡도록
- 외부 연동은 동기 blocking 최소화
- 모듈 경계와 문서/마이그레이션 영향까지 같이 본다
- 한 곳에서만 쓰이는 리터럴은 상수화하지 않는다

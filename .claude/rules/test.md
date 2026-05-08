# 테스트 작성 패턴

> **이 문서의 범위**: Kotest DescribeSpec + Mockk 기반 UseCase / Controller / Repository 테스트의 canonical 형태.
>
> **테스트 일반 원칙 (커버리지·외부 의존 mock 등)**: `conventions.md` "테스트" 섹션
> **컨벤션 플러그인 (`crispinlab.kotest`)**: `architecture.md`

## 프레임워크

- **Kotest DescribeSpec** + **Mockk**.
- `crispinlab.kotest` 컨벤션 플러그인이 자동 적용 — 각 모듈에서 별도 설정 불필요.
- JUnit 어노테이션(`@Test`) 사용 금지. DescribeSpec DSL 로 통일.

## 디렉토리·이름

```
src/test/kotlin/
├── com/crispinlab/space/
│   ├── application/usecase/page/
│   │   └── PageGettingUseCaseTest.kt        # UseCase 테스트
│   ├── adapter/web/page/
│   │   └── PageGettingControllerTest.kt     # Controller 테스트
│   └── adapter/persistence/page/
│       └── ExposedPageRepositoryTest.kt     # Repository 테스트
└── com/crispinlab/space/testsupport/
    ├── Fixtures.kt                          # basicXxx() 함수 모음
    └── Dummies.kt                           # 상수 (DUMMY_INSTANT 등)
```

- 테스트 클래스 이름: 대상 + `Test` (단수). Spec 마다 한 대상.
- `testsupport` 패키지에 Fixture / Dummy 를 모은다 — 흩어지면 같은 객체가 여러 모양으로 만들어짐.

## Fixture / Dummy

```kotlin
package com.crispinlab.space.testsupport

import com.crispinlab.space.domain.page.Page
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.domain.page.Visibility
import com.crispinlab.space.domain.user.UserId
import java.time.Instant

object Dummies {
    val DUMMY_INSTANT: Instant = Instant.parse("2025-01-01T00:00:00Z")
}

object Fixtures {
    fun basicPage(
        id: PageId = PageId(1L),
        authorId: UserId = UserId(100L),
        title: String = "테스트 페이지",
        body: String = "본문",
        visibility: Visibility = Visibility.DRAFT,
    ): Page =
        Page(
            id = id,
            authorId = authorId,
            title = title,
            body = body,
            visibility = visibility,
            createdAt = Dummies.DUMMY_INSTANT,
            updatedAt = Dummies.DUMMY_INSTANT,
        )
}
```

- **Fixture 는 `basicXxx()` 형태**. 이름·타입 변경에 자동 따라가게 모든 필드를 default 로.
- **시간은 `Instant.now()` 금지** — `DUMMY_INSTANT` 사용. 테스트가 시계에 흔들리지 않게.
- **Fixture 함수는 static import** — `Fixtures.basicPage()` 가 아니라 `basicPage()` (`conventions.md` "Fixture 함수는 static import").
- Fixture 안에서 다른 Fixture 를 호출해 합성하는 건 OK (`basicComment(page = basicPage())`).

## UseCase 테스트

```kotlin
package com.crispinlab.space.application.usecase.page

import com.crispinlab.common.exception.NotFoundException
import com.crispinlab.space.application.port.incoming.page.PageGetting.Request
import com.crispinlab.space.application.port.outgoing.page.PageRepository
import com.crispinlab.space.domain.page.PageId
import com.crispinlab.space.testsupport.DummyTransactionProvider
import com.crispinlab.space.testsupport.Fixtures.basicPage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class PageGettingUseCaseTest : DescribeSpec({
    val pageRepository = mockk<PageRepository>()
    val useCase = PageGettingUseCase(pageRepository, DummyTransactionProvider())

    describe("페이지 단건 조회") {
        it("정상적으로 조회한다") {
            val page = basicPage(title = "오늘의 회고")
            every { pageRepository.findBy(page.id) } returns page

            val result = useCase.perform(basicRequest(pageId = page.id.value.toString()))

            result.title shouldBe "오늘의 회고"
        }

        it("페이지가 없으면 NotFoundException") {
            every { pageRepository.findBy(any()) } returns null

            shouldThrow<NotFoundException> {
                useCase.perform(basicRequest())
            }
        }
    }
}) {
    companion object {
        fun basicRequest(
            pageId: String = "1",
            currentUserId: String = "100",
        ): Request =
            Request(pageId = pageId, currentUserId = currentUserId)
    }
}
```

### 작성 시 따져볼 것

- `describe` 가 도메인 컨텍스트를 잡는다 — `it` 에는 같은 접두어 반복 금지 (`conventions.md` "컨텍스트 중복 금지").
- 의존은 모두 `mockk<>()` 로. **외부 네트워크/DB 의존을 끌어들이지 않는다**.
- Request 도 Spec 안 `companion object` 의 `basicRequest()` 로 모아둔다 — 같은 Spec 안에서 `.copy()` 대비가 가능하도록 `data class` 라면 `.copy(...)`, 일반 `class` (UseCase Request) 면 default 인자로.
- 검증은 `shouldBe` / `shouldThrow` 로. `assertEquals` 등 JUnit API 금지.

### 검증 케이스 최소 셋

| 케이스 | 무엇을 본다 |
|--------|------------|
| 정상 흐름 | Result 의 핵심 필드, Repository.save 호출 인자 |
| 외부 의존 없음 | Repository 가 `null` 반환 → 적절한 예외 |
| 권한/소유자 분기 | `findBy + takeIf` 패턴 (`usecase-implementation.md`) 이 NotFoundException 로 응답하는지 |
| 입력 형식 오류 | Request 생성 자체가 실패 (`asPageId` 변환 실패) — Spec 안에서 `shouldThrow<IllegalArgumentException>` |

## Controller 테스트

Spring REST 라우팅·serialization 까지 검증하려면 `@WebMvcTest` 또는 `MockMvc` 기반 Spec. UseCase 는 mock.

```kotlin
package com.crispinlab.space.adapter.web.page

import com.crispinlab.space.application.port.incoming.page.PageGetting
import com.crispinlab.space.application.port.incoming.page.PageGetting.Result
import com.crispinlab.space.testsupport.Dummies.DUMMY_INSTANT
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(PageGettingController::class)
class PageGettingControllerTest(
    private val mockMvc: MockMvc,
    @MockkBean private val useCase: PageGetting,
) : DescribeSpec({
    describe("GET /v1/pages/{pageId}") {
        it("200 과 함께 페이지 정보를 반환한다") {
            every { useCase.perform(any()) } returns Result(
                pageId = "1",
                title = "테스트",
                visibility = "DRAFT",
                updatedAt = DUMMY_INSTANT,
            )

            mockMvc.get("/v1/pages/1") {
                header("X-User-Id", "100")
            }.andExpect {
                status { isOk() }
                jsonPath("$.title") { value("테스트") }
            }
        }
    }
})
```

- `@WebMvcTest` 로 controller layer 만 띄운다 — 다른 빈은 mock.
- 검증은 path / status / jsonPath 까지. 단순 메서드 호출 검증으로 그치지 않는다 (`conventions.md` "테스트가 실제 회귀를 잡는가").

## Repository 테스트

Exposed 어댑터는 **실제 DB(H2 in-memory)** 와 같이 돌리는 게 회귀 가치가 있다. mock 으로 어댑터 스스로의 SQL 매핑을 검증할 수 없음.

```kotlin
class ExposedPageRepositoryTest : DescribeSpec({
    // crispinlab.kopring.exposed 가 H2 + Spring Boot 테스트 컨텍스트 제공
    // SchemaUtils.create(Pages) 를 BeforeSpec 에 두고
    // transaction { ... } 안에서 save / findBy / delete 검증
})
```

- 실제 SQL/매핑이 회귀 지점이라 단위 mock 보다 통합 테스트 가치가 큼.
- 시간 포맷, enum 직렬화, null 컬럼 매핑 등 매핑 비대칭이 잡힌다.

## 부팅 컨텍스트 로드 테스트 (`app` 모듈)

`app` 모듈의 `@SpringBootApplication` 이 정상 부팅 가능한지 자체를 회귀로 둔다 — auto-config 누락, profile 잘못 활성, 의존성 누락 같은 케이스가 사전 차단된다.

```kotlin
@SpringBootTest
class ApplicationTest : DescribeSpec({
    extensions(SpringExtension())

    describe("Spring 컨텍스트") {
        it("정상적으로 로드된다") {
            // 컨텍스트 로드 자체가 회귀 케이스
        }
    }
})
```

- `crispinlab.kopring.test` 가 `kotest-extensions-spring` (group `io.kotest`, kotest 본 버전과 동기화) 을 testImplementation 으로 자동 wiring.
- `SpringExtension` 은 kotest 6 부터 일반 `class` — `extensions(SpringExtension())` 처럼 인스턴스화해서 등록 (`SpringExtension` 단일 object 호출 금지).
- spec 본문에서 `@Autowired` 생성자 주입은 **하지 말 것**. SpringExtension 은 spec 인스턴스화 *후* 적용되므로 생성자 인자 자리에 빈을 채워주지 못해 `SpecInstantiationException` 발생. 빈을 단언하려면 ApplicationContext 까지 띄운 후 별도 spec 에서.

## 자주 빠뜨리는 것

- **`Instant.now()` 직접 사용** — flaky 의 원인. `DUMMY_INSTANT` 로.
- **Fixture 기본값에 설명 주석** — `// title = "테스트", visibility = DRAFT`. 테스트 이름과 `.copy()` 대비로 충분 (`conventions.md` "Fixture 기본값에 설명 주석 금지").
- **`@Test` 어노테이션 혼용** — DescribeSpec 으로 통일.
- **Mock 검증을 `verify` 만으로** — 호출됐는지뿐 아니라 인자도 검증. 기왕이면 결과 필드 검증으로.
- **Controller 테스트가 service 단위** — 직접 controller 인스턴스를 만들어 메서드 호출하면 라우팅·serialization 회귀가 빠진다. `MockMvc` 로.
- **테스트마다 다른 Fixture 모양** — Fixture 함수를 새로 만들지 말고 `basicXxx().copy(...)` 로 차이만 표현. 같은 Spec 안에서 6 줄짜리 entity 를 반복 생성하는 건 신호.

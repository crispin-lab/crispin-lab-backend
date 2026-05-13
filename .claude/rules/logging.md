# 로깅

> **이 문서의 범위**: 요청 로깅 / MDC / logger 호출의 어디까지 어떻게 쓸지의 규약.
>
> **검증·예외 처리 메시지**: `error-messages.md`
> **모듈 경계**: `architecture.md` (`lab-common` pure Kotlin / `lab-common-infra` Spring 어댑터)

## 핵심 원칙

로깅은 **framework hook 의 책임**이다. 비즈니스 로직에 logger 호출이 끼어들면 흐름이 흐려지고, 어떤 로그가 어떤 요청에서 나왔는지 추적이 어려워진다. 본 프로젝트는 servlet filter + logback pattern 으로 요청 단위 로깅을 끝내고, UseCase·도메인은 로그를 모른다.

## 룰

### 1. UseCase / 도메인 코드는 logger 호출 0 개

`com.crispinlab.<domain>.application.*`, `com.crispinlab.<domain>.domain.*` 안에서 `org.slf4j.Logger` import 금지.

```kotlin
// BAD: UseCase 안에서 직접 로그
class PageEditingUseCase(...) : PageEditing {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun perform(request: Request): Result {
        log.info("editing page ${request.pageId}")     // 금지
        ...
    }
}

// GOOD: UseCase 는 비즈니스 흐름만. 로깅은 filter + exception handler 가 담당
class PageEditingUseCase(...) : PageEditing {
    override fun perform(request: Request): Result = ...
}
```

도메인 동작에 대한 audit / 분석 로깅이 정말 필요해지면 **그 시점에** 별도 outbound port (`AuditPort` 등) 로 추상화 — UseCase 안에 raw logger 가 들어가는 것은 피한다.

### 2. MDC 키는 `LogContext.Mdc` 의 상수로만 접근

`com.crispinlab.common.logging.LogContext.Mdc` 의 상수 (`TRACE_ID`, `METHOD`, `PATH`) 만 MDC 키로 사용. 문자열 리터럴 금지. logfmt 메시지 필드명은 `LogContext.Field` 에 따로 둔다 — MDC 키와 메시지 placeholder 의 역할이 코드만 봐도 분리되어야 한다.

```kotlin
// BAD
MDC.put("traceId", id)

// GOOD
MDC.put(LogContext.Mdc.TRACE_ID, id)
log.info("{}={} {}={}", LogContext.Field.STATUS, status, LogContext.Field.DURATION_MS, durationMs)
```

키 이름이 logback pattern (`%X{traceId}`) 과 한 곳에 묶여, 키 리네임이 컴파일러로 잡힌다.

### 3. 요청/응답 본문 로깅 금지

PII 마스킹 부담과 메모리 사용 부담이 크다. 본문이 정말 필요한 디버깅 케이스 (4xx/5xx 시 본문 dump 등) 는 별도 티켓으로 옵션화한다.

### 4. MDC set/clear 는 `TraceContextFilter` 만

`TRACE_ID / METHOD / PATH` MDC 는 요청 진입 시 `TraceContextFilter` 가 put, 응답 종료 후 진입 전 MDC 상태로 복원한다. 다른 곳에서 직접 set/remove 하면 의미 분산 + 누수 위험.

`TraceContextFilter` 는 진입 시점의 MDC snapshot (`MDC.getCopyOfContextMap()`) 을 캡처하고 finally 에서 `MDC.setContextMap(...)` 으로 복원하므로, 호출 측이 사전에 MDC 에 넣어둔 값은 보존된다. 다만 본 룰 자체가 "외부에서 만지지 않는다" 라서 일반적으로 호출 시점 MDC 는 비어 있다.

추후 `userId` 같은 컨텍스트가 추가되어도 같은 원칙 — 전용 filter / interceptor 가 책임을 갖는다. UseCase 안에서 `MDC.put` 하지 않는다.

### 5. 구조화 필드는 logfmt 형식

로그 메시지의 구조화 필드는 `key=value` (logfmt) 로 작성. 새 필드를 도입하면 `LogContext.Field` 에 상수로 함께 정의해 callsite 가 문자열을 직접 쓰지 않게 한다.

```kotlin
// GOOD — TraceContextFilter 의 완료 라인
accessLog.info(
    "{}={} {}={}",
    LogContext.Field.STATUS, response.status,
    LogContext.Field.DURATION_MS, durationMs
)
```

### 6. `X-Trace-Id` 응답 헤더는 항상 포함

응답에 traceId 가 헤더로 박혀 있어야 클라이언트 / 운영이 특정 요청 로그를 역추적할 수 있다. 본문에 traceId 를 넣어 노출할지는 별도 결정 (현재는 헤더만으로 충분).

### 7. URL path 에 sensitive 식별자를 박지 않는다

access log 가 `request.requestURI` 를 그대로 로그/MDC 에 흘리므로, path 자체에 이메일·토큰·내부 키 같은 sensitive 값이 들어오면 그대로 노출된다. 이런 식별자는 path variable 이 아니라 request body 또는 별도 헤더로 받는다 (요청 본문은 본 시스템이 로그에 쓰지 않는다).

```text
// BAD
GET /v1/users/by-email/foo@example.com
GET /v1/sessions/abc123secret/refresh

// GOOD
GET /v1/users/{userId}
POST /v1/sessions/refresh   (token in body or Authorization header)
```

### 8. access log 는 전용 logger name 사용

요청 완료 라인은 `LoggerFactory.getLogger("http.access")` 로 분리해 둔다 (룰 1번 "logger 이름은 클래스 기반" 의 예외 — access log 는 운영에서 별도 retention / appender 로 분리할 가능성이 높다). UseCase 안에서 임의 문자열 logger 이름을 만드는 것은 여전히 금지.

## traceId 결정 규칙

| 입력 | 동작 |
|------|------|
| 요청에 `traceparent` 헤더 없음 | 자체 발급 — 16-hex (`SecureRandom` 8 bytes) |
| 유효한 `traceparent` (W3C, 32-hex trace-id) | 그 trace-id 를 그대로 사용 |
| malformed `traceparent` | 자체 발급으로 fallback (요청 거부 안 함) |
| all-zero trace-id (`00000...0`) | W3C invalid — 자체 발급으로 fallback |

- 16-hex 와 32-hex 가 섞이는 것은 의도된 결과 — 외부에서 W3C 표준으로 들어온 trace 는 이어 받고, 자체 발급은 로그 한 줄 길이를 줄이기 위한 선택. 둘 다 MDC 의 같은 키 (`traceId`) 로 흐른다.
- `traceparent` 의 version 필드는 현재 `00..ff` 어떤 값이든 통과시키고 trace-id 만 추출한다 (W3C "future version compatibility" 가이드). spec 엄격 적용으로 `00` 만 허용하고 싶어지면 정규식 한 곳을 좁힌다.
- `traceparent` 의 `traceflags` (sampled flag) 는 현재 무시 — sampling 결정은 본 스코프 외 (Micrometer Tracing / OpenTelemetry 도입 시 별도 티켓).

## 자주 빠뜨리는 것

- **UseCase 안 `runCatching { ... }.onFailure { log.error(...) }`** — 예외는 핸들러 (`GlobalExceptionHandler` 등) 가 응답 매핑과 같이 로깅. UseCase 는 도메인 예외를 그대로 위로 던진다 (`error-messages.md`).
- **`MDC.put` 후 `MDC.remove` 누락** — 누수의 원인. `TraceContextFilter` 외부에서 MDC 를 만지면 finally + clear 가 빠지기 쉽다. 그래서 외부에서 만지지 않는 것이 첫 번째 룰.
- **logger 이름을 임의 문자열로 (`LoggerFactory.getLogger("audit")`)** — 클래스 기반 (`LoggerFactory.getLogger(javaClass)`) 으로. 예외는 본 룰 8번의 access logger.
- **요청 본문을 `info` 로 한 줄 dump** — PII / 메모리 / 4xx 디버깅 유혹. 룰 3번 그대로 — 본문은 로깅하지 않는다.
- **`TraceContextFilter` 를 `@Component` / `@Bean` 으로 추가 노출** — `FilterRegistrationBean` 으로만 등록한다. 빈 추가 등록 시 Spring Boot 가 Filter 빈을 자동 servlet 등록해 매 요청 traceId 가 두 번 갱신된다.

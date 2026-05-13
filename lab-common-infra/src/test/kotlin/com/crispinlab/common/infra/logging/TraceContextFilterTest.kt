package com.crispinlab.common.infra.logging

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.crispinlab.common.logging.LogContext.Mdc
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import jakarta.servlet.http.HttpServletResponse
import org.hamcrest.Matchers.matchesPattern
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

class TraceContextFilterTest :
    DescribeSpec({
        val mockMvc: MockMvc =
            MockMvcBuilders
                .standaloneSetup(StubController())
                .addFilters<StandaloneMockMvcBuilder>(TraceContextFilter(TraceIdGenerator()))
                .build()

        val accessLogger = LoggerFactory.getLogger(TraceContextFilter.ACCESS_LOGGER_NAME) as Logger
        val accessLogAppender = ListAppender<ILoggingEvent>().apply { start() }

        beforeSpec { accessLogger.addAppender(accessLogAppender) }
        afterSpec { accessLogger.detachAppender(accessLogAppender) }
        beforeEach {
            MDC.clear()
            accessLogAppender.list.clear()
        }
        afterEach { MDC.clear() }

        describe("traceId 결정") {
            it("traceparent 헤더가 없으면 16-hex 를 자동 발급해 X-Trace-Id 로 반환한다") {
                mockMvc
                    .perform(get("/ping"))
                    .andExpect(status().isOk)
                    .andExpect(header().string("X-Trace-Id", matchesPattern(SELF_ISSUED_PATTERN)))
            }

            it("유효한 traceparent 가 들어오면 32-hex trace-id 부분을 그대로 사용한다") {
                mockMvc
                    .perform(
                        get("/ping")
                            .header("traceparent", VALID_TRACEPARENT)
                    ).andExpect(status().isOk)
                    .andExpect(header().string("X-Trace-Id", INBOUND_TRACE_ID))
            }

            it("malformed traceparent 가 들어오면 자동 발급으로 fallback 한다") {
                mockMvc
                    .perform(
                        get("/ping")
                            .header("traceparent", "garbage")
                    ).andExpect(status().isOk)
                    .andExpect(header().string("X-Trace-Id", matchesPattern(SELF_ISSUED_PATTERN)))
            }

            it("all-zero trace-id 가 들어오면 자동 발급으로 fallback 한다") {
                mockMvc
                    .perform(
                        get("/ping")
                            .header("traceparent", ALL_ZERO_TRACEPARENT)
                    ).andExpect(status().isOk)
                    .andExpect(header().string("X-Trace-Id", matchesPattern(SELF_ISSUED_PATTERN)))
            }
        }

        describe("응답 완료 access log") {
            it("응답 종료 후 status/durationMs 한 줄이 INFO 로 찍힌다") {
                mockMvc.perform(get("/ping")).andExpect(status().isOk)

                accessLogAppender.list shouldHaveSize 1
                accessLogAppender.list[0].formattedMessage shouldMatch
                    Regex("""^status=200 durationMs=\d+$""")
            }

            it("4xx 응답에서도 동일하게 status 가 access log 로 흐른다") {
                mockMvc.perform(get("/missing")).andExpect(status().isNotFound)

                accessLogAppender.list shouldHaveSize 1
                accessLogAppender.list[0].formattedMessage shouldContain "status=404"
            }
        }

        describe("MDC 누수") {
            it("요청 종료 후 traceId/method/path MDC 가 모두 비어 있다") {
                mockMvc.perform(get("/ping")).andExpect(status().isOk)

                MDC.get(Mdc.TRACE_ID) shouldBe null
                MDC.get(Mdc.METHOD) shouldBe null
                MDC.get(Mdc.PATH) shouldBe null
            }

            it("진입 전 MDC 값이 있으면 요청 종료 후 그 값이 복원된다") {
                MDC.put("preexisting", "outer")

                mockMvc.perform(get("/ping")).andExpect(status().isOk)

                MDC.get("preexisting") shouldBe "outer"
                MDC.get(Mdc.TRACE_ID) shouldBe null
            }

            it("연속 두 요청 간 traceId 가 서로 영향 없이 독립적으로 발급된다") {
                val first =
                    mockMvc
                        .perform(get("/ping"))
                        .andReturn()
                        .response
                        .getHeader("X-Trace-Id")
                MDC.get(Mdc.TRACE_ID) shouldBe null

                val second =
                    mockMvc
                        .perform(get("/ping"))
                        .andReturn()
                        .response
                        .getHeader("X-Trace-Id")

                first.shouldNotBeNull()
                second.shouldNotBeNull()
                first.shouldMatch(Regex(SELF_ISSUED_PATTERN))
                second.shouldMatch(Regex(SELF_ISSUED_PATTERN))
                (first == second) shouldBe false
            }
        }
    }) {
    @RestController
    class StubController {
        @GetMapping("/ping")
        fun ping(): String = "ok"

        @GetMapping("/missing")
        fun missing(response: HttpServletResponse) {
            response.status = 404
        }
    }

    companion object {
        private const val SELF_ISSUED_PATTERN: String = "^[0-9a-f]{16}$"
        private const val INBOUND_TRACE_ID: String =
            "0af7651916cd43dd8448eb211c80319c"
        private const val VALID_TRACEPARENT: String =
            "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"
        private const val ALL_ZERO_TRACEPARENT: String =
            "00-00000000000000000000000000000000-b7ad6b7169203331-01"
    }
}
